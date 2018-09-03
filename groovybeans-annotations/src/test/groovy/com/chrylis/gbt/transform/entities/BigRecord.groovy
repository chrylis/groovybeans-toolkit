package com.chrylis.gbt.transform.entities

import com.chrylis.gbt.annotation.GbtId
import com.chrylis.gbt.annotation.Subrecord

import groovy.transform.CompileStatic

@GbtId(type = UUID)
@CompileStatic
class BigRecord {

    @Subrecord
    SmallRecordA a

    @Subrecord(createIfMissing = false)
    SmallRecordB b

    BigRecord() {
        println 'hello'
    }

    BigRecord(Closure closure) {
        closure.call(this)
    }
}
