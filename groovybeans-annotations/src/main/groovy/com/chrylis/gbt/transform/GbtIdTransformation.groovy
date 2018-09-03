package com.chrylis.gbt.transform

import javax.persistence.Id

import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.PropertyNode
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.transform.AnnotationCollectorTransform
import org.codehaus.groovy.transform.GroovyASTTransformation

import com.chrylis.gbt.annotation.GbtId

import groovy.transform.CompileStatic

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
@CompileStatic
class GbtIdTransformation extends PropertyAddingTransformation<GbtId> {

    // to support unrolling auxiliary annotations onto the generated field
    private final AnnotationCollectorTransform collectorProcessor = new AnnotationCollectorTransform()

    @Override
    Class<GbtId> annotationClass() { GbtId }

    @Override
    protected Class<?> identifyingAnnotation() { Id }

    @Override
    protected void doAddProperty(AnnotationNode annotation, ClassNode target, ClassNode type, String name) {
        PropertyNode property = addProperty(target, type, name)
        FieldNode field = property.field

        List<ClassNode> annotationCollectors = GbtUtils.getAnnotationParameterClassesValue(annotation, "annotationCollectors")

        // if there were any collectors specified, manually apply their annotations to the new field
        annotationCollectors
            .collect { new AnnotationNode(it) }
            .collect { collectorProcessor.visit(it, it, field, sourceUnit) }
            .each { field.addAnnotations(it) }
    }
}
