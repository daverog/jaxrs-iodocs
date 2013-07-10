package org.daverog.jaxrs.iodocs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The value placed in a Boolean parameter's field in an I/O Docs form.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface IoDocsDefaultBoolean {
	boolean value();
}
