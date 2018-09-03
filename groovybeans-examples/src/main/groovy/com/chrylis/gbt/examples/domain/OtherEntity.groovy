package com.chrylis.gbt.examples.domain

import javax.persistence.Entity
import javax.persistence.OneToOne

import com.chrylis.gbt.annotation.TwoWayRelationship

import groovy.transform.CompileStatic

@Entity
@CompileStatic
class OtherEntity {
    @TwoWayRelationship(mappedBy = "foo")
    @OneToOne
    SimpleEntity bar

    Integer asdf
}
