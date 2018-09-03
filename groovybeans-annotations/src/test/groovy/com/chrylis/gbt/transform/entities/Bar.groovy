package com.chrylis.gbt.transform.entities

import groovy.transform.CompileStatic

import com.chrylis.gbt.annotation.TwoWayRelationship

@CompileStatic
class Bar {
    @TwoWayRelationship
    Foo foo
}
