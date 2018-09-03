package com.chrylis.gbt.transform

import groovy.transform.CompileStatic

import java.lang.reflect.Field
import java.time.Instant

import javax.persistence.Column
import javax.persistence.Id
import javax.persistence.OneToOne

import org.codehaus.groovy.control.MultipleCompilationErrorsException

import spock.lang.Specification

import com.chrylis.gbt.transform.annotationcollectors.AddColumnName
import com.chrylis.gbt.transform.annotationcollectors.AddOneToOne
import com.chrylis.gbt.annotation.GbtId

class GbtIdTransformationTest extends Specification {

    @GbtId
    @CompileStatic
    class Identified {
    }

    def 'annotated class with default parameters has Long "id" field'() {
        given:
            Field field = Identified.getDeclaredField('id')

        expect:
            Long == field.type
            field.getAnnotation(Id)
    }

    def 'getters and setters for version field work'() {
        given:
            Long randomNumber = new Random().nextLong()

            def v = new Identified()
            v.setId(randomNumber)

        expect:
            randomNumber == v.getId()
            randomNumber == v.@id
    }

    @CompileStatic
    Class makeIdentifiedClass(Class type, String name, String body = '') {
        new GroovyClassLoader().parseClass("""
            @groovy.transform.CompileStatic
            @com.chrylis.gbt.annotation.GbtId(type=$type.name, name="$name")
            class Temp { $body }""")
    }

    def 'parameters are applied correctly'(Class type, String fieldName) {
        given:
            Field field = makeIdentifiedClass(type, fieldName, 'Short id').getDeclaredField(fieldName)

        expect:
            type == field.type
            field.getAnnotation(Id)

        where:
            type    | fieldName
            int     | 'foobar'
            Long    | 'helloWorld'
            Instant | 'asdf'
    }

    def 'conflicting field names produce an error'() {
        when:
            makeIdentifiedClass(Long, 'id', 'Long id')

        then:
            MultipleCompilationErrorsException ex = thrown()
            ex.message.contains('@GbtId')
            ex.message.contains('already exists')

            1 == ex.errorCollector.errorCount
    }

    def 'conflicting annotation produces an error'() {
        when:
            makeIdentifiedClass(Long, 'id', '@javax.persistence.Id Integer foo')

        then:
            MultipleCompilationErrorsException ex = thrown()
            ex.message.contains('@GbtId')
            ex.message.contains('existing @Id')

            1 == ex.errorCollector.errorCount
    }

    @CompileStatic
    Class makeClassWithAnnotationCollectors(List<Class> annotationCollectors) {
        String parameter

        if(annotationCollectors.empty) {
            parameter = ''
        } else if(annotationCollectors.size() == 1) {
            parameter = 'annotationCollectors = ' + annotationCollectors[0].name
        } else {
            parameter = 'annotationCollectors = [' +
                annotationCollectors.collect { it.name }.join(',') + ']'
        }

        new GroovyClassLoader().parseClass("""
            @groovy.transform.CompileStatic
            @com.chrylis.gbt.annotation.GbtId($parameter)
            class Temp {}""")
    }

    def 'annotation collector correctly copies annotations'(List<Class> parameters, int numAnnotations) {
        given:
            Field field = makeClassWithAnnotationCollectors(parameters).getDeclaredField('id')

        expect:
            Long == field.type
            field.getAnnotation(Id)
            numAnnotations == field.annotations.length

        if(field.annotations.length == 3) {
            assert field.getAnnotation(Column).name() == 'foobar'
            assert field.getAnnotation(OneToOne).mappedBy() == 'asdf'
        }

        where:
            parameters                   || numAnnotations
            []                           || 1
            [AddOneToOne]                || 2
            [AddOneToOne, AddColumnName] || 3
    }
}
