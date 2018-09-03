package com.chrylis.gbt.annotation

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

import org.codehaus.groovy.transform.GroovyASTTransformationClass

import com.chrylis.gbt.transform.GbtIdTransformation

/**
 * Adds a JPA {@link Id} property to the annotated type.
 *
 * @author Christopher Smith
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@GroovyASTTransformationClass(classes = GbtIdTransformation)
public @interface GbtId {

    /**
     * (Optional) The ID type to use for this entity.
     */
    Class<?> type() default Long

    /**
     * (Optional) The name of the ID property.
     */
    String name() default "id"

    /**
     * Any annotation collectors whose values should be copied to the generated ID property.
     * In particular, generator annotations are likely candidates, allowing project-wide
     * meta-annotations such as {@code @MyProjectEntity} to be defined and reused.
     */
    Class<? extends GroovyObject>[] annotationCollectors() default []
}
