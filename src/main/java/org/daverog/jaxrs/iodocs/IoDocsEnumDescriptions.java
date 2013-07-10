package org.daverog.jaxrs.iodocs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * User-facing descriptions corresponding to enumeration values allowed 
 * when using a parameter in a I/O Docs form.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface IoDocsEnumDescriptions {
	String[] value();
}
