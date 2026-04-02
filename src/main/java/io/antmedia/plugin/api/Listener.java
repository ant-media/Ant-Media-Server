package io.antmedia.plugin.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class for automatic listener registration.
 * AMS inspects which interfaces the class implements (IStreamListener, IFrameListener,
 * IPacketListener, IServerListener) and registers it accordingly.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Listener {
	Scope scope() default Scope.APPLICATION;
}
