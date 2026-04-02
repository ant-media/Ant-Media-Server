package io.antmedia.plugin.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a POJO class as a plugin configuration holder.
 * AMS loads values from the file on disk (or uses defaults from the class fields).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Configuration {
	/**
	 * Filename on disk, e.g. "my-plugin.config"
	 */
	String file();

	Scope scope() default Scope.APPLICATION;
}
