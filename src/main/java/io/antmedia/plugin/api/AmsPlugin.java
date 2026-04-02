package io.antmedia.plugin.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the main entry point class of a V2 plugin.
 * Exactly one class per plugin JAR must have this annotation.
 * Metadata (name, version, author) comes from MANIFEST.MF attributes.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AmsPlugin {
}
