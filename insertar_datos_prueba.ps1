# Script para insertar productos y ventas de prueba y verificar paginacion
$ErrorActionPreference = "Stop"

Write-Host "Obteniendo token de administrador..." -ForegroundColor Cyan
$tokenResponse = Invoke-RestMethod -Uri "http://localhost:8081/api/dev/token?role=ADMIN" -Method Get
$token = $tokenResponse.token
$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type"  = "application/json"
}

# 1. Lista de productos a insertar
$productosPrueba = @(
    @{ nombre = "Aceite Vegetal 1L"; precio = 8500; stockInicial = 120; umbralMinimo = 15 },
    @{ nombre = "Leche Entera 1L"; precio = 4200; stockInicial = 150; umbralMinimo = 20 },
    @{ nombre = "Cafe Molido 500g"; precio = 14500; stockInicial = 80; umbralMinimo = 10 },
    @{ nombre = "Azucar Blanca 1kg"; precio = 4800; stockInicial = 120; umbralMinimo = 15 },
    @{ nombre = "Sal Refinada 1kg"; precio = 1800; stockInicial = 90; umbralMinimo = 10 },
    @{ nombre = "Lentejas 500g"; precio = 3800; stockInicial = 70; umbralMinimo = 12 },
    @{ nombre = "Garbanzo 500g"; precio = 4100; stockInicial = 65; umbralMinimo = 10 },
    @{ nombre = "Pasta Spaghetti 500g"; precio = 2900; stockInicial = 110; umbralMinimo = 20 },
    @{ nombre = "Atun en Aceite 170g"; precio = 6200; stockInicial = 95; umbralMinimo = 15 },
    @{ nombre = "Sardinas en Tomate 425g"; precio = 5100; stockInicial = 60; umbralMinimo = 10 },
    @{ nombre = "Harina de Maiz 1kg"; precio = 3600; stockInicial = 130; umbralMinimo = 25 },
    @{ nombre = "Harina de Trigo 1kg"; precio = 3400; stockInicial = 100; umbralMinimo = 15 },
    @{ nombre = "Chocolate de Mesa 500g"; precio = 7800; stockInicial = 85; umbralMinimo = 15 },
    @{ nombre = "Avena en Hojuelas 500g"; precio = 3900; stockInicial = 90; umbralMinimo = 12 },
    @{ nombre = "Galletas Saltinas Taco"; precio = 2500; stockInicial = 140; umbralMinimo = 30 },
    @{ nombre = "Jabon de Bano 3x110g"; precio = 8900; stockInicial = 75; umbralMinimo = 10 },
    @{ nombre = "Detergente en Polvo 1kg"; precio = 9500; stockInicial = 80; umbralMinimo = 15 },
    @{ nombre = "Papel Higienico 4 rollos"; precio = 6500; stockInicial = 120; umbralMinimo = 20 },
    @{ nombre = "Blanqueador Desinfectante 1L"; precio = 3100; stockInicial = 70; umbralMinimo = 15 },
    @{ nombre = "Esponja Multiusos 3 und"; precio = 3500; stockInicial = 100; umbralMinimo = 20 }
)

Write-Host "Insertando $($productosPrueba.Count) productos..." -ForegroundColor Cyan
foreach ($p in $productosPrueba) {
    try {
        $bodyJson = [System.Text.Encoding]::UTF8.GetBytes(($p | ConvertTo-Json))
        $res = Invoke-RestMethod -Uri "http://localhost:8082/api/productos" -Method Post -Headers $headers -Body $bodyJson
        Write-Host "  [OK] Producto creado: $($res.nombre) (ID: $($res.id))" -ForegroundColor Green
    } catch {
        Write-Host "  [AVISO] No se pudo crear $($p.nombre): $($_.Exception.Message)" -ForegroundColor Yellow
    }
}

# 2. Consultar catálogo completo para usar IDs reales en las ventas
Write-Host "Consultando catálogo de productos para ventas..." -ForegroundColor Cyan
$resCatalogo = Invoke-RestMethod -Uri "http://localhost:8082/api/productos?page=0&size=200" -Method Get -Headers $headers
$catalogo = if ($resCatalogo.contenido) { $resCatalogo.contenido } else { $resCatalogo }
Write-Host "Total productos disponibles en catálogo: $($catalogo.Count)" -ForegroundColor Green

if ($catalogo.Count -lt 2) {
    Write-Error "No hay suficientes productos para registrar ventas."
    exit 1
}

# 3. Insertar 20 ventas variadas
Write-Host "Insertando 20 ventas de prueba..." -ForegroundColor Cyan
$random = New-Object System.Random

for ($i = 1; $i -le 20; $i++) {
    # Elegir entre 1 y 3 productos al azar
    $cantItems = $random.Next(1, 4)
    $items = @()
    $productosElegidos = $catalogo | Get-Random -Count $cantItems

    foreach ($prod in $productosElegidos) {
        $cantidad = $random.Next(1, 5)
        $items += @{
            productoId     = $prod.id
            cantidad       = $cantidad
            precioUnitario = [double]$prod.precio
        }
    }

    $ventaBody = @{ items = $items } | ConvertTo-Json -Depth 5
    try {
        $resVenta = Invoke-RestMethod -Uri "http://localhost:8081/api/ventas" -Method Post -Headers $headers -Body $ventaBody
        Write-Host "  [OK] Venta #$i registrada - Total: `$($resVenta.total) - Items: $($items.Count) (ID: $($resVenta.id))" -ForegroundColor Green
    } catch {
        Write-Host "  [AVISO] Error al registrar venta #${i}: $($_.Exception.Message)" -ForegroundColor Yellow
    }
    Start-Sleep -Milliseconds 150
}

Write-Host "`n=== Resumen de Paginación ===" -ForegroundColor Cyan

# Consultar primera página de productos
$resProd = Invoke-RestMethod -Uri "http://localhost:8082/api/productos?page=0&size=10" -Method Get -Headers $headers
Write-Host "Productos (Página 0, Size 10):"
Write-Host "  totalElementos:        $($resProd.totalElementos)"
Write-Host "  totalPaginas:          $($resProd.totalPaginas)"
Write-Host "  tienePaginaSiguiente:  $($resProd.tienePaginaSiguiente)"
Write-Host "  tienePaginaAnterior:   $($resProd.tienePaginaAnterior)"
Write-Host "  elementos en contenido:$($resProd.contenido.Count)"

# Consultar primera página de ventas
$resVenta = Invoke-RestMethod -Uri "http://localhost:8081/api/ventas?page=0&size=10" -Method Get -Headers $headers
Write-Host "`nVentas (Página 0, Size 10):"
Write-Host "  totalElementos:        $($resVenta.totalElementos)"
Write-Host "  totalPaginas:          $($resVenta.totalPaginas)"
Write-Host "  tienePaginaSiguiente:  $($resVenta.tienePaginaSiguiente)"
Write-Host "  tienePaginaAnterior:   $($resVenta.tienePaginaAnterior)"
Write-Host "  elementos en contenido:$($resVenta.contenido.Count)"

Write-Host "`nDatos de prueba insertados con éxito." -ForegroundColor Green
