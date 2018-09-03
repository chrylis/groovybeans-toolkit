package com.chrylis.gbt.transform.entities

import com.chrylis.gbt.annotation.SubrecordOf

import groovy.transform.CompileStatic

@SubrecordOf(BigRecord)
@CompileStatic
class SmallRecordA {
    String value
}
