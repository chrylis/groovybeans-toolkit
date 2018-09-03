package com.chrylis.gbt.transform

import static org.codehaus.groovy.ast.ClassHelper.make

import java.lang.annotation.Annotation

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation

import groovy.transform.CompileStatic

@CompileStatic
abstract class GbtTransformation<T extends Annotation> extends AbstractASTTransformation {

    /**
     * @return the annotation type that triggers this transformation
     */
    abstract Class<T> annotationClass()

    /**
     * @return the AST node representing this transformation's annotation type
     */
    final ClassNode annotationClassNode() {
        make(annotationClass())
    }

    /**
     * @return The annotation in the form usually applied to a target (such as {@code @GjtFoo}).
     */
    final String getAnnotationName() {
        '@' + annotationClassNode().nameWithoutPackage
    }

    /**
     * @return Whether this transformation that has a target of {@code TYPE} can be applied only
     * to classes or also to interfaces.
     */
    abstract boolean canApplyToClassOnly()

    /**
     * Adds a compile error and returns true if the target class type is an interface.
     * Used by transformations that should only be applied to classes, such as ones
     * that manipulate properties.
     *
     * @param targetNode the node that the annotation was applied to
     * @return {@code true} if the target node is an interface
     */
    protected final boolean targetNodeIsInterface(ClassNode targetNode) {
        return !checkNotInterface(targetNode, getAnnotationName());
    }

    @Override
    final void visit(ASTNode[] nodes, SourceUnit source) {
        init(nodes, source)

        AnnotationNode annotation = (AnnotationNode) nodes[0]
        if (annotationClassNode() != annotation.classNode) { return }

        AnnotatedNode annotated = (AnnotatedNode) nodes[1]

        if (canApplyToClassOnly()) {
            if (!(annotated instanceof ClassNode)) {
                // This shouldn't happen because the annotation should specify a correct @Target.
                addError("Annotation ${getAnnotationName()} should only be applicable to classes, but it was applied to a non-type element.", annotation)
                return
            } else if (targetNodeIsInterface((ClassNode) annotated)) {
                // The class-only annotation was applied to an interface.
                return
            }
        }

        doVisit(annotation, annotated);
    }

    protected abstract void doVisit(AnnotationNode annotation, AnnotatedNode annotated)
}
