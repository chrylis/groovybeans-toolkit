package com.chrylis.gbt.transform

import org.apache.commons.lang3.RandomStringUtils
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.expr.ConstantExpression

import com.chrylis.gbt.annotation.GbtVersion

import spock.lang.Specification

class GbtUtilsTest extends Specification {

    ClassNode cNode = new ClassNode(GbtVersion)
    AnnotationNode node = new AnnotationNode(cNode)

    def 'can retrieve default value for annotation node member'() {
        expect:
            'version' == GbtUtils.getAnnotationParameterDefault(node, 'name', String)
            Long == GbtUtils.getAnnotationParameterDefault(node, 'type', Class)

        when:
            GbtUtils.getAnnotationParameterDefault(node, 'invalid', String)

        then:
            IllegalArgumentException ex = thrown()
            ex.message.contains('invalid')
    }

    def 'can retrieve default value for annotation type member'() {
        expect:
            'version' == GbtUtils.getAnnotationParameterDefault(GbtVersion, "name", String)
            Long == GbtUtils.getAnnotationParameterDefault(GbtVersion, "type", Class)
    }

    def 'can retrieve String value or default for annotation node member'() {
        given:
            def fieldName = RandomStringUtils.randomAlphabetic(12)

        expect:
            'version' == GbtUtils.getAnnotationParameterStringValue(node, 'name')

        when:
            node.addMember('name', new ConstantExpression(fieldName))

        then:
            fieldName == GbtUtils.getAnnotationParameterStringValue(node, 'name')
    }

    def 'can generate setter name'(String fieldName, String setterName) {
        expect:
            setterName == GbtUtils.setterName(fieldName)

        where:
            fieldName || setterName
            "foo"     || "setFoo"
            "Bar"     || "setBar"
            "URL"     || "setURL"
            "_asdf"   || "set_asdf"
            "_Jay"    || "set_Jay"
    }
}
