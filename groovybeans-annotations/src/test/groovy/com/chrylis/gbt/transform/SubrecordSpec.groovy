package com.chrylis.gbt.transform

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric

import com.chrylis.gbt.transform.entities.BigRecord
import com.chrylis.gbt.transform.entities.SmallRecordA
import com.chrylis.gbt.transform.entities.SmallRecordB

import spock.lang.Specification

class SubrecordSpec extends Specification {

    def 'standard case works as expected'() {
        given: 'the test record setup'
        String aValue = randomAlphanumeric(9)
        String bValue = randomAlphanumeric(10)
        SmallRecordA aReference

        when:

        BigRecord big = new BigRecord({ aReference = it.a }).tap { a.value = aValue }
        SmallRecordB b = new SmallRecordB(value: bValue)

        then: 'a default A has been assigned to Big'
        big.a
        big == big.a.mainRecord

        and: 'no default B has been assigned to Big'
        ! big.b

        and: 'the constructor body was executed after defaults were set'
        aReference.is(big.a)

        when:
        big.b = b

        then:
        big == b.mainRecord
    }
}
