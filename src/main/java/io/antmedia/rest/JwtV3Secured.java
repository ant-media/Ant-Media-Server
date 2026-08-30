package io.antmedia.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.ws.rs.NameBinding;

/**
 * Marks a v3 REST endpoint as protected by the new v3 JWT authorization scheme
 * ({@link JWTFilterV3}). Only methods/classes annotated with this run through the
 * filter, so v2 endpoints are unaffected.
 */
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface JwtV3Secured {
}
