package com.chrylis.gbt.transform

import static com.chrylis.gbt.transform.GbtUtils.fieldDefaultValue
import static com.chrylis.gbt.transform.GbtUtils.findAnnotatedMembers
import static com.chrylis.gbt.transform.GbtUtils.getAnnotationParameterDefault
import static com.chrylis.gbt.transform.GbtUtils.getAnnotationParameterStringValue
import static org.codehaus.groovy.ast.ClassHelper.make

import java.lang.annotation.Annotation

import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.PropertyNode
import org.codehaus.groovy.ast.expr.Expression

import groovy.transform.CompileStatic

@CompileStatic
abstract class PropertyAddingTransformation<T extends Annotation> extends GbtTransformation {

    /**
     * These transformations add properties to the class in question; it doesn't make sense to apply elsewhere.
     */
    @Override
    boolean canApplyToClassOnly() { true }

    /**
     * @return the annotation, such as {@code @Version} or {@code @Id},
     *         that identifies the property this transformation manages
     */
    protected abstract Class<?> identifyingAnnotation()

    /**
     * A {@link ClassNode} representing the identifying annotation.
     */
    protected final ClassNode IDENTIFYING_ANNOTATION_CLASS_NODE = make(identifyingAnnotation())

    /**
     * The name of the parameter of the triggering annotation that specifies the type of the property.
     */
    public static final String ANNOTATION_TYPE_PARAMETER_NAME = "type"

    /**
     * A {@link ClassNode} representing the default type to use when creating the property.
     */
    protected final ClassNode DEFAULT_TYPE_CLASS_NODE =
        make(getAnnotationParameterDefault(annotationClass(), ANNOTATION_TYPE_PARAMETER_NAME, Class))

    /**
     * The name of the parameter of the triggering annotation that specifies the name of the property.
     */
    public static final String ANNOTATION_NAME_PARAMETER_NAME = "name"

    /**
     * The default name for the property.
     */
    protected final String DEFAULT_PROPERTY_NAME =
        getAnnotationParameterDefault(annotationClass(), ANNOTATION_NAME_PARAMETER_NAME, String.class);

    /**
     * Verifies that the field name and identifying annotation have no conflicts in the annotated
     * class, and if not, delegates to {@link #doAddProperty(AnnotationNode, ClassNode, ClassNode, String)}.
     *
     * @param annotationNode
     *            the annotation that triggered this transformation
     * @param annotatedNode
     *            the class node that the annotation was applied to
     */
    @Override
    protected final void doVisit(AnnotationNode annotationNode, AnnotatedNode annotatedNode) {
        ClassNode entity = (ClassNode) annotatedNode;
        String fieldName = getAnnotationParameterStringValue(annotationNode, ANNOTATION_NAME_PARAMETER_NAME)
        ClassNode fieldType = getMemberClassValue(annotationNode, ANNOTATION_TYPE_PARAMETER_NAME, DEFAULT_TYPE_CLASS_NODE)

        if (fieldNameConflicts(entity, fieldName) || annotationConflicts(entity)) {
            return;
        }

        doAddProperty(annotationNode, entity, fieldType, fieldName);
    }

    /**
     * Actually add the property, after the target class has been determined to be free of conflicts.
     * The default implementation creates a public property with a private backing field initialized
     * to the default value for that field's type and annotates the field with the identifying annotation
     * with no parameters.
     *
     * @param annotation
     *            the annotation that triggered this transformation (which may have additional options that the specific
     *            transformation wants to read)
     * @param target
     *            the class to add the property to
     * @param type
     *            the type of the property to add
     * @param name
     *            the name of the property to add
     */
    protected void doAddProperty(AnnotationNode annotation, ClassNode target, ClassNode type, String name) {
        addProperty(target, type, name)
    }

    /**
     * Adds the specified property to a class, using the specified initial value and modifiers
     * for the property. The backing field is made private and is annotated with the identifying
     * annotation.
     *
     * @param target
     *            the class to add the property to
     * @param type
     *            the type of the property
     * @param name
     *            the name of the property
     * @param modifiers
     *            the access modifiers for the property, such as {@code ACC_PUBLIC}
     * @param initialValue
     *            the value to be assigned on initialization
     * @return the created propety
     */
    protected final PropertyNode addProperty(ClassNode target, ClassNode type, String name, int modifiers, Expression initialValue) {
        PropertyNode property = target.addProperty(name, modifiers, type, initialValue, null, null)

        FieldNode field = property.field
        field.modifiers = ACC_PRIVATE
        field.addAnnotation(new AnnotationNode(IDENTIFYING_ANNOTATION_CLASS_NODE))

        return property
    }

    /**
     * Adds the specified property to a class, making it public and using the default initial value
     * for the property's type. The backing field is made private and is annotated with the
     * identifying annotation.
     *
     * @param target
     *            the class to add the property to
     * @param type
     *            the type of the property
     * @param name
     *            the name of the property
     * @return the created property
     */
    protected final PropertyNode addProperty(ClassNode target, ClassNode type, String name) {
        addProperty(target, type, name, ACC_PUBLIC, fieldDefaultValue(type))
    }

    /**
     * Checks whether the named field already exists on the target class, and if so, adds
     * an appropriate error.
     *
     * @param target
     *            the class to add the field to
     * @param name
     *            the name of the field
     * @return {@code true} if a field of the same name already exists
     */
    protected boolean fieldNameConflicts(final ClassNode target, final String name) {
        final FieldNode existingField = target.getField(name)

        // no conflict, so we're fine
        if (existingField == null) {
            return false
        }

        StringBuilder error = new StringBuilder("Can't add field named ").append(name)
            .append(" for @").append(annotationClass().simpleName)
            .append(" because it already exists")

        if (target != existingField.owner) {
            error.append(" on superclass ").append(existingField.owner.name)
        }

        addError(error.toString(), existingField)
        return true
    }

    /**
     * Checks whether the class already contains a member annotated with the identifying
     * annotation, and if so, adds an appropriate error.
     *
     * @param target
     *            the class to add the field to
     * @return {@code true} if a class member is already annotated with the identifying annotation
     */
    protected boolean annotationConflicts(ClassNode target) {
        List<AnnotatedNode> conflicts = findAnnotatedMembers(target, IDENTIFYING_ANNOTATION_CLASS_NODE)

        if (conflicts.isEmpty()) {
            return false
        }

        String error = "Class annotated with @${annotationClass().simpleName} can't have an existing @${identifyingAnnotation().simpleName}"

        conflicts.each { addError(error, it) }
        return true
    }
}
