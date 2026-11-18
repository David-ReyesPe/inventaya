package com.inventaya.inventario.config;

import com.inventaya.inventario.exception.ForbiddenException;
import com.inventaya.inventario.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/**
 * Aplica las restricciones de {@link RolRequerido}. Las excepciones que lanza se traducen
 * a ErrorResponse en el GlobalExceptionHandler, igual que las de los controladores.
 */
@Component
public class RolInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RolRequerido restriccion = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), RolRequerido.class);
        if (restriccion == null) {
            restriccion = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RolRequerido.class);
        }
        if (restriccion == null) {
            return true;
        }

        Object atributo = request.getAttribute(UsuarioAutenticado.ATRIBUTO_REQUEST);
        if (!(atributo instanceof UsuarioAutenticado usuario)) {
            throw new UnauthorizedException("No se envio un token JWT valido.");
        }

        boolean autorizado = Arrays.asList(restriccion.value()).contains(usuario.rol());
        if (!autorizado) {
            throw new ForbiddenException("El usuario autenticado no tiene permisos para esta operacion.");
        }
        return true;
    }
}
