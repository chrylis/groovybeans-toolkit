package com.chrylis.gbt.transform

import static jdk.internal.org.objectweb.asm.Opcodes.*
import groovy.transform.CompileStatic

import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.FieldNode

import spock.lang.Specification

import com.chrylis.gbt.transform.entities.Bar
import com.chrylis.gbt.transform.entities.Foo
import com.chrylis.gbt.annotation.TwoWayRelationship

class TwoWayRelationshipTransformationTest extends Specification {

    def 'synthetic setter naming'(String fieldName, String setterName) {
        given:
            FieldNode field = new FieldNode(fieldName, 0, ClassHelper.DYNAMIC_TYPE, null, null)

        expect:
            setterName == TwoWayRelationshipTransformation.syntheticSetterName(field)

        where:
            fieldName || setterName
            'foo'     || '$gbt_setFoo'
            'Bar'     || '$gbt_setBar'
            'URL'     || '$gbt_setURL'
            '_asdf'   || '$gbt_set_asdf'
            '_Jay'    || '$gbt_set_Jay'

    }

    def 'setter works from both directions'() {
        given:
            Foo f1 = new Foo()
            Bar b1 = new Bar()

            Foo f2 = new Foo()
            Bar b2 = new Bar()

        when:
            f1.bar = b1
            b2.foo = f2

        then:
            f1 == b1.foo
            b2 == f2.bar

        when:
            f1.bar = null
            b2.foo = null

        then:
            b1.foo == null
            f2.bar == null
    }

    def 'cross-updates work'() {
        given:
            Foo f1 = new Foo()
            Bar b1 = new Bar(foo: f1)

            Foo f2 = new Foo()
            Bar b2 = new Bar(foo: f2)

        expect:
            b1 == f1.bar
            b2 == f2.bar

        when:
            f1.bar = b2

        then:
            b2 == f1.bar
            f1 == b2.foo
            null == f2.bar
            null == b1.foo
    }
}
