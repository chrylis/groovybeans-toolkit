package com.chrylis.gbt.annotation

import java.lang.annotation.Documented
import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

import org.codehaus.groovy.transform.GroovyASTTransformationClass

import com.chrylis.gbt.transform.SubrecordOfTransformation

/**
 * Indicates that the annotated class is a <em>subrecord</em> of another entity.
 *
 * This annotation generates an ID field that mirrors the type of the main entity's ID and a
 * JPA one-to-one relationship that uses the ID as a foreign key. Additionally, if this class is not
 * already annotated with {@link Entity}, it will be added.
 *
 * @see Subrecord Subrecord for a detailed explanation of the semantics of this pattern
 *
 * @author Christopher Smith
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
@Documented
@GroovyASTTransformationClass(classes = SubrecordOfTransformation)
@interface SubrecordOf {

    /**
     * @return the type of the main record for the subrecord entity
     */
    Class<?> value() default void.class

    /**
     * @return the name of the ID field created for a subrecord entity
     */
    String idField() default "id"

    /**
     * @return the name of the relationship field created on the subrecord entity
     */
    String mainRecordField() default "mainRecord"
}
