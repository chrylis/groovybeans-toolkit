package com.chrylis.gbt.transform.annotationcollectors

import groovy.transform.AnnotationCollector

import javax.persistence.OneToOne

@AnnotationCollector
@OneToOne(mappedBy = "asdf")
@interface AddOneToOne {
}
