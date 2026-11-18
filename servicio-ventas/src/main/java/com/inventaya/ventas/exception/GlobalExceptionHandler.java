package com.inventaya.ventas.exception;

import com.inventaya.ventas.dto.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarBodyInvalido(MethodArgumentNotValidException ex) {
        String mensaje = Stream.concat(
                        ex.getBindingResult().getFieldErrors().stream(),
                        ex.getBindingResult().getGlobalErrors().stream())
                .map(error -> error.getDefaultMessage() != null
                        ? error.getDefaultMessage()
                        : "Los datos enviados no son validos.")
                .distinct()
                .collect(Collectors.joining(" "));
        if (mensaje.isBlank()) {
            mensaje = "Los datos enviados no son validos.";
        }
        return responder(HttpStatus.BAD_REQUEST, mensaje);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> manejarViolacionDeRestriccion(ConstraintViolationException ex) {
        String mensaje = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .distinct()
                .collect(Collectors.joining(" "));
        return responder(HttpStatus.BAD_REQUEST, mensaje.isBlank() ? "Parametros invalidos." : mensaje);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> manejarJsonIlegible(HttpMessageNotReadableException ex) {
        log.debug("Body no legible: {}", ex.getMessage());
        return responder(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es un JSON valido o contiene valores no permitidos.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> manejarTipoInvalido(MethodArgumentTypeMismatchException ex) {
        return responder(HttpStatus.BAD_REQUEST,
                "El parametro '" + ex.getName() + "' no tiene un formato valido.");
    }

    @ExceptionHandler(ValidacionNegocioException.class)
    public ResponseEntity<ErrorResponse> manejarValidacionNegocio(ValidacionNegocioException ex) {
        return responder(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> manejarNoAutenticado(UnauthorizedException ex) {
        return responder(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> manejarSinPermisos(ForbiddenException ex) {
        return responder(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> manejarNoEncontrado(ResourceNotFoundException ex) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> manejarStockInsuficiente(InsufficientStockException ex) {
        return responder(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(AlreadyCancelledException.class)
    public ResponseEntity<ErrorResponse> manejarVentaYaAnulada(AlreadyCancelledException ex) {
        return responder(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Cubre la familia de excepciones propias de Spring MVC (recurso estatico inexistente,
     * metodo HTTP no soportado, media type no soportado, parametro requerido ausente...),
     * respetando su codigo de estado en lugar de degradarlas a 500.
     */
    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ErrorResponse> manejarErroresDeSpringMvc(ErrorResponseException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return responder(status, status == HttpStatus.NOT_FOUND
                ? "El recurso solicitado no existe."
                : ex.getBody().getDetail());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarErrorInesperado(Exception ex) {
        log.error("Error inesperado procesando la solicitud", ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrio un error inesperado en el servidor.");
    }

    private ResponseEntity<ErrorResponse> responder(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(ErrorResponse.de(status.value(), mensaje));
    }
}
