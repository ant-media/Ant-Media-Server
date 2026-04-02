package io.antmedia.plugin.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field for AMS dependency injection in @Listener and @Rest classes.
 * AMS resolves by type from a well-defined set of injectable objects.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Inject {
}
