package com.inventaya.ventas.service;

import com.inventaya.ventas.dto.AnulacionInput;
import com.inventaya.ventas.dto.VentaInput;
import com.inventaya.ventas.dto.VentaItemInput;
import com.inventaya.ventas.dto.VentaResponse;
import com.inventaya.ventas.entity.ProductoStock;
import com.inventaya.ventas.entity.Venta;
import com.inventaya.ventas.exception.InsufficientStockException;
import com.inventaya.ventas.exception.ResourceNotFoundException;
import com.inventaya.ventas.exception.ValidacionNegocioException;
import com.inventaya.ventas.kafka.VentaEventProducer;
import com.inventaya.ventas.mapper.VentaMapper;
import com.inventaya.ventas.repository.ProductoStockRepository;
import com.inventaya.ventas.repository.VentaRepository;
import com.inventaya.ventas.repository.VentaSpecifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VentaService {

    private static final Logger log = LoggerFactory.getLogger(VentaService.class);

    private final VentaRepository ventaRepository;
    private final ProductoStockRepository productoStockRepository;
    private final VentaEventProducer ventaEventProducer;

    public VentaService(VentaRepository ventaRepository,
                        ProductoStockRepository productoStockRepository,
                        VentaEventProducer ventaEventProducer) {
        this.ventaRepository = ventaRepository;
        this.productoStockRepository = productoStockRepository;
        this.ventaEventProducer = ventaEventProducer;
    }

    /**
     * RF2: registra la venta completa y publica "venta-realizada".
     *
     * El descuento efectivo del stock lo hace servicio-inventario al consumir el evento; aqui
     * solo se valida que la venta sea cumplible, para poder rechazarla con 409 antes de
     * registrarla. No hay ventas parciales: si falla un item, se rechaza la venta entera.
     *
     * @throws ValidacionNegocioException  si algun item referencia un producto inexistente (400).
     * @throws InsufficientStockException  si algun producto no tiene stock suficiente (409).
     */
    @Transactional
    public VentaResponse registrar(VentaInput input) {
        Map<UUID, Integer> unidadesPorProducto = agruparUnidadesPorProducto(input.items());
        validarDisponibilidad(unidadesPorProducto);

        List<Venta.LineaNueva> lineas = input.items().stream()
                .map(item -> new Venta.LineaNueva(item.productoId(), item.cantidad(), item.precioUnitario()))
                .toList();

        Venta venta = ventaRepository.save(Venta.registrar(lineas));
        log.info("Venta registrada: {} con {} item(s), total {}",
                venta.getId(), venta.getItems().size(), venta.getTotal());

        ventaEventProducer.publicarVentaRealizada(venta);
        return VentaMapper.aResponse(venta);
    }

    @Transactional(readOnly = true)
    public Page<VentaResponse> listar(Instant desde, Instant hasta, Pageable pageable) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ValidacionNegocioException(
                    "El rango de fechas es invalido: 'desde' no puede ser posterior a 'hasta'.");
        }
        return ventaRepository.findAll(VentaSpecifications.enRangoDeFechas(desde, hasta), pageable)
                .map(VentaMapper::aResponse);
    }

    @Transactional(readOnly = true)
    public VentaResponse obtener(UUID id) {
        return VentaMapper.aResponse(buscarOFallar(id));
    }

    /**
     * RF7: marca la venta como ANULADA (nunca se elimina ni se edita) y publica
     * "venta-anulada" para que Inventario revierta el stock de forma asincrona.
     */
    @Transactional
    public VentaResponse anular(UUID id, AnulacionInput input) {
        Venta venta = buscarOFallar(id);
        venta.anular(input.motivo().trim());
        log.info("Venta anulada: {} (motivo: {})", venta.getId(), venta.getMotivoAnulacion());

        ventaEventProducer.publicarVentaAnulada(venta);
        return VentaMapper.aResponse(venta);
    }

    private Venta buscarOFallar(UUID id) {
        return ventaRepository.findWithItemsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro la venta solicitada."));
    }

    /**
     * Un mismo producto puede venir en varias lineas de la misma venta; el stock se valida
     * contra la suma de sus unidades, no linea por linea.
     */
    private Map<UUID, Integer> agruparUnidadesPorProducto(List<VentaItemInput> items) {
        return items.stream().collect(Collectors.groupingBy(
                VentaItemInput::productoId,
                LinkedHashMap::new,
                Collectors.summingInt(VentaItemInput::cantidad)));
    }

    private void validarDisponibilidad(Map<UUID, Integer> unidadesPorProducto) {
        List<UUID> productosEnOrdenDeBloqueo = unidadesPorProducto.keySet().stream().sorted().toList();

        for (UUID productoId : productosEnOrdenDeBloqueo) {
            ProductoStock producto = productoStockRepository.bloquearPorId(productoId)
                    .orElseThrow(() -> new ValidacionNegocioException(
                            "El producto " + productoId + " no existe en el catalogo."));

            int unidadesSolicitadas = unidadesPorProducto.get(productoId);
            if (unidadesSolicitadas > producto.getStock()) {
                throw new InsufficientStockException(
                        "Stock insuficiente para el producto " + productoId + ".");
            }
        }
    }
}
