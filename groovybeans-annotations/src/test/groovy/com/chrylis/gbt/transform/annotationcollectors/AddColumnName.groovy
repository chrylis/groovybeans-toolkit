package com.chrylis.gbt.transform.annotationcollectors

import groovy.transform.AnnotationCollector

import javax.persistence.Column

@AnnotationCollector
@Column(name = "foobar")
@interface AddColumnName {
}
