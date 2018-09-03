package com.chrylis.gbt.transform

import static org.codehaus.groovy.ast.ClassHelper.*
import static java.util.Collections.emptyList

import java.lang.annotation.Annotation
import java.lang.reflect.Method

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.PropertyNode
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.ListExpression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.control.messages.SyntaxErrorMessage
import org.codehaus.groovy.syntax.SyntaxException
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.AbstractASTTransformation

import groovy.transform.CompileStatic

@CompileStatic
final class GbtUtils {
    /**
     * Returns the default value that should be assigned to the field if none is specified.
     *
     * @param fieldType
     *            the type of the field being added
     * @return {@code null} for reference types, zero for numeric primitives, and {@code false} for boolean primitives
     */
    static ConstantExpression fieldDefaultValue(ClassNode fieldType) {
        if (!isPrimitiveType(fieldType)) {
            return new ConstantExpression(null);
        }

        if (isNumberType(fieldType) || fieldType == char_TYPE) {
            return new ConstantExpression(0, true);
        }

        return new ConstantExpression(false, true);
    }

    static List<FieldNode> findAnnotatedFields(ClassNode onClass, ClassNode annotation) {
        onClass.fields.findAll { it.getAnnotations(annotation) }
    }

    static List<PropertyNode> findAnnotatedProperties(ClassNode onClass, ClassNode annotation) {
        onClass.properties.findAll { it.getAnnotations(annotation) }
    }

    static List<MethodNode> findAnnotatedMethods(ClassNode onClass, ClassNode annotation) {
        onClass.methods.findAll { it.getAnnotations(annotation) }
    }

    static List<AnnotatedNode> findAnnotatedMembers(ClassNode onClass, ClassNode annotation) {
        def members = []
        members.addAll(findAnnotatedFields(onClass, annotation))
        members.addAll(findAnnotatedProperties(onClass, annotation))
        members.addAll(findAnnotatedMethods(onClass, annotation))
        return members
    }

    static <T> T getAnnotationParameterDefault(AnnotationNode annotation, String name, Class<T> parameterType) {
        Class<?> nodeClass = annotation.classNode.typeClass
        if(!nodeClass.annotation) {
            throw new IllegalArgumentException("type $annotation is not an annotation type")
        }

        // safe because we checked above
        return getAnnotationParameterDefault((Class<? extends Annotation>) nodeClass, name, parameterType)
    }

    static <T> T getAnnotationParameterDefault(Class<? extends Annotation> annotationType, String name, Class<T> parameterType) {
        if(!annotationType.annotation) {
            throw new IllegalArgumentException("type $annotationType is not an annotation type")
        }

        try {
            Method method = annotationType.getMethod(name)
            return (T) method.defaultValue
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("parameter $name does not exist on annotation type $annotationType")
        } catch (SecurityException e) {
            throw new RuntimeException(e)
        }
    }

    static String getAnnotationParameterStringValue(AnnotationNode annotation, String name) {
        String value = AbstractASTTransformation.getMemberStringValue(annotation, name)
        if(value == null) {
            value = getAnnotationParameterDefault(annotation, name, String)
        }
        return value
    }

    static List<ClassNode> getAnnotationParameterClassesValue(AnnotationNode annotation, String name) {
        Expression member = annotation.getMember(name)
        if(!member) {
            return emptyList()
        }

        if (member instanceof ClassExpression) {
            return [member.type]
        }

        return ((ListExpression) member).expressions*.type
    }

    static <T> T getAnnotationParameterValueOrDefault(AnnotationNode annotation, String name, Class<T> parameterType) {
        Expression member = annotation.getMember(name)
        if (member instanceof PropertyExpression) {
            member = ((PropertyExpression) member).property
        }
        if (member instanceof ConstantExpression) {
            return ((ConstantExpression) member).value
        }

        return getAnnotationParameterDefault(annotation, name, parameterType)
    }

    static String setterName(String fieldName) {
        StringBuilder name = new StringBuilder("set").append(fieldName);
        name.setCharAt(3, Character.toUpperCase(name.charAt(3)));
        return name.toString();
    }

    static String setterName(FieldNode field) {
        setterName(field.name)
    }

    static void forceTransformAtPhase(ClassNode target, Class<? extends ASTTransformation> transform, AnnotationNode site, CompilePhase phase) {
        target.getTransforms(phase).with {
            putIfAbsent(transform, new LinkedHashSet<>())
            get(transform).add(site)
        }
    }

    static void manuallyExecuteTransform(Class<? extends ASTTransformation> transform, AnnotationNode annotation, AnnotatedNode annotated, SourceUnit sourceUnit) {
        ASTTransformation instance

        try {
            instance = transform.newInstance()
        } catch (InstantiationException | IllegalAccessException e) {
            sourceUnit.errorCollector.addErrorAndContinue(new SyntaxErrorMessage(
                new SyntaxException(
                    "Could not instantiate transformation $transform.name",
                    annotation.lineNumber, annotation.columnNumber,
                    annotation.lastLineNumber, annotation.lastColumnNumber
                ),
                sourceUnit
            ))

            return
        }

        instance.visit([annotation, annotated] as ASTNode[], sourceUnit)
    }
}
