package com.chrylis.gbt.examples.domain

import com.chrylis.gbt.annotation.SubrecordOf

import groovy.transform.CompileStatic

@CompileStatic
@SubrecordOf(MainRecord)
class SubRecord {
    String secret
}
