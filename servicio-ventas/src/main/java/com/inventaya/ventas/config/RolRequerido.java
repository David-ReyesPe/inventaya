package com.inventaya.ventas.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Restringe un endpoint a los roles indicados. Sin esta anotacion basta un JWT valido
 * (cualquier rol); con ella, un rol distinto recibe 403.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RolRequerido {

    RolUsuario[] value();
}
