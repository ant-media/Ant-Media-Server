package io.antmedia.plugin.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a plugin REST endpoint.
 * Use standard JAX-RS annotations (@GET, @POST, @Path, etc.) for methods.
 * AMS handles discovery and dependency injection.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Rest {
	Scope scope() default Scope.APPLICATION;
}
