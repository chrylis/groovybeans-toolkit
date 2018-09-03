package com.chrylis.gbt.examples.domain

import javax.persistence.Id

import com.chrylis.gbt.annotation.Subrecord

import groovy.transform.CompileStatic

@CompileStatic
class MainRecord {

    @Id
    Short id

    @Subrecord
    SubRecord foobar
}
