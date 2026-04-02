package io.antmedia.plugin.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a field in a @Configuration class to describe a config option.
 * Used for UI rendering and documentation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigOption {
	String description();
	String defaultValue() default "";
	Widget widget() default Widget.TEXT;
}
