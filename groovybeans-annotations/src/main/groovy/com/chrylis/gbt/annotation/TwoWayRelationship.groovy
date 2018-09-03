package com.chrylis.gbt.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.codehaus.groovy.transform.GroovyASTTransformationClass;

import com.chrylis.gbt.transform.TwoWayRelationshipTransformation

/**
 * Implements a managed two-way relationship between corresponding properties in different objects.
 *
 * <p>A longstanding difficulty in JPA and other data models that express bidirectional relationships
 * between objects is ensuring that both sides of a relationship are kept in sync. Simply writing
 * the setters on each class to call the corresponding setter on the other results in infinite
 * recursion, and most other approaches consist of applying business rules that direct either that
 * one side of the relationship must be the primary (so, for example, {@code A#setB(B)} is called
 * but never {@code B#setA(A)}), or that the setters must always be called in pairs.</p>
 *
 * <p>Both of these approaches are highly error-prone, as they require humans to consistently apply
 * rules; tend to fail when setters are called by framework methods or other infrastructure that is
 * unaware of the specific business rule; and have problems with edge cases, such as failing to
 * release the newly-single object when an object in an existing relationship is replaced.</p>
 *
 * <p>This transformation automates the two-way relationship between corresponding properties.
 * In addition to keeping both ends of the relationship in sync, it includes logic for handling
 * setting a relationship property to null (breaking the relationship) and for replacing an existing
 * relationship with a new partner (ensuring that the previous partner's backreference is nulled
 * out). This logic is inserted as the property's ordinary setter, so that client code can simply
 * call {@code a.setB(b)} without knowing about the management.</p>
 *
 * <p>The transformation only operates on one end of the relationship at a time, but it confirms
 * that the backreference has a matching annotation. If so, it generates a <em>synthetic</em>
 * setter method called {@code gbt_<setterName>} that consists of solely the usual assignment
 * to {@code this.field}. This method is used to perform the updates on other objects involved in
 * the operation without invoking recursion. The main setter is then defined with this algorithm:
 *
 * <ul>
 * <li>Let the setter parameter be called {@code newValue} and the existing value of the field
 *     be {@code oldValue}.</li>
 * <li>If {@code newValue === oldValue}, then exit early; we don't need to update anything.</li>
 * <li>If {@code oldValue} is not null, set the backreference on {@code oldValue} to null so that it
 *     no longer points at {@code this}.</li>
 * <li>Set {@code this.field} to {@code newValue}.</li>
 * <li>If {@code newValue} is null, then there is no corresponding object, so exit.</li>
 * <li>Otherwise, there is a corresponding object. Examine the backreference on the corresponding
 *     object to see whether it was already in a relationship. If it was, null out the reference
 *     <em>from the previous partner of this class</em> to {@code newValue}; it is now "single".</li>
 * <li>Set the backreference on {@code newValue} to {@code this}.</li>
 * </ul></p>
 * @author Christopher Smith
 *
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.CLASS)
@GroovyASTTransformationClass(classes = TwoWayRelationshipTransformation)
@interface TwoWayRelationship {

    /**
     * The name of the corresponding property on the other end of the relationship. This is usually
     * automatically detected, but it might need to be explicitly defined if more than one
     * relationship is defined for the same pair of types.
     */
    String mappedBy() default ""
}
