package com.chrylis.gbt.transform

import javax.persistence.Version

import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.transform.GroovyASTTransformation

import com.chrylis.gbt.annotation.GbtVersion

import groovy.transform.CompileStatic

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
@CompileStatic
class GbtVersionTransformation extends PropertyAddingTransformation<GbtVersion> {

    @Override
    Class<GbtVersion> annotationClass() { GbtVersion }

    @Override
    protected Class<?> identifyingAnnotation() { Version }
}
