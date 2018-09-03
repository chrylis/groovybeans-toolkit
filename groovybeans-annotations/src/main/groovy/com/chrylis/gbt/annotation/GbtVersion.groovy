package com.chrylis.gbt.annotation

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

import org.codehaus.groovy.transform.GroovyASTTransformationClass

import com.chrylis.gbt.transform.GbtVersionTransformation

/**
 * Adds a JPA {@code @Version} property to the annotated type.
 *
 * @author Christopher Smith
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@GroovyASTTransformationClass(classes = GbtVersionTransformation)
public @interface GbtVersion {

    /**
     * (Optional) The type to use for the version. The default is set as a very safe {@code Long},
     * but {@code Instant} is a useful alternative if Java 8 time types are available.
     */
    Class<?> type() default Long

    /**
     * (Optional) The name to use for the version property.
     */
    String name() default "version"
}
