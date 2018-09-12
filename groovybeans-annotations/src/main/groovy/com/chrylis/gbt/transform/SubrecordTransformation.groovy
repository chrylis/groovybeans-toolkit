package com.chrylis.gbt.transform;

import static com.chrylis.gbt.transform.GbtUtils.getAnnotationParameterClassesValue
import static com.chrylis.gbt.transform.GbtUtils.getAnnotationParameterDefault
import static com.chrylis.gbt.transform.GbtUtils.getAnnotationParameterStringValue
import static com.chrylis.gbt.transform.GbtUtils.getAnnotationParameterValueOrDefault
import static com.chrylis.gbt.transform.SubrecordOfTransformation.MAIN_RECORD_FIELD_PARAMETER_NAME
import static org.codehaus.groovy.ast.ClassHelper.make;
import static org.codehaus.groovy.ast.expr.ConstantExpression.PRIM_TRUE
import static org.codehaus.groovy.ast.tools.GeneralUtils.callThisX
import static org.codehaus.groovy.ast.tools.GeneralUtils.classX
import static org.codehaus.groovy.ast.tools.GeneralUtils.constX
import static org.codehaus.groovy.ast.tools.GeneralUtils.ctorX
import static org.codehaus.groovy.ast.tools.GeneralUtils.propX
import static org.codehaus.groovy.ast.tools.GeneralUtils.stmt

import javax.persistence.CascadeType
import javax.persistence.FetchType
import javax.persistence.OneToOne

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.transform.GroovyASTTransformation;

import com.chrylis.gbt.annotation.Subrecord
import com.chrylis.gbt.annotation.SubrecordOf
import com.chrylis.gbt.annotation.TwoWayRelationship

import groovy.transform.CompileStatic

/**
 * Establishes a subrecord relationship with a property.
 *
 * @see Subrecord
 * @author christopher
 *
 */
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
@CompileStatic
class SubrecordTransformation extends GbtTransformation<Subrecord> {

    @Override
    Class<Subrecord> annotationClass() { Subrecord }

    @Override
    boolean canApplyToClassOnly() { false }

    protected static final ClassNode SUBRECORD_OF_TYPE = make(SubrecordOf)

    protected static final ClassNode ONE_TO_ONE_TYPE = make(OneToOne)

    @Override
    protected void doVisit(AnnotationNode annotation, AnnotatedNode annotated) {
        FieldNode field = (FieldNode) annotated
        ClassNode mainRecord = field.declaringClass
        ClassNode subrecord = field.type

        AnnotationNode reflexive = reflexiveAnnotation(mainRecord, subrecord, annotation)
        if(reflexive == null) {
            return
        }
        String mappedBy = getAnnotationParameterStringValue(reflexive, MAIN_RECORD_FIELD_PARAMETER_NAME)

        AnnotationNode twrAnnotation = new AnnotationNode(make(TwoWayRelationship))
        twrAnnotation.addMember('mappedBy', constX(mappedBy))
        field.addAnnotation(twrAnnotation)
        // Because of ordering, the mainRecord field may not be generated on the subrecord class
        // at the time that we would normally invoke TWR, so directly add the generated code
        // to the main class here.
        FieldNode toBeGeneratedOnSubrecord = new FieldNode(mappedBy, ACC_PRIVATE, mainRecord, subrecord, null)
        TwoWayRelationshipTransformation.addGeneratedCode(field, toBeGeneratedOnSubrecord)

        AnnotationNode otoAnnotation = new AnnotationNode(ONE_TO_ONE_TYPE)
        Expression cascadeValue = annotation.getMember("cascade")
        if(!cascadeValue) {
            cascadeValue = propX(
                classX(make(CascadeType)),
                constX(getAnnotationParameterDefault(annotation, "cascade", CascadeType[])[0])
            )
        }
        otoAnnotation.addMember("cascade", cascadeValue)

        otoAnnotation.addMember("fetch",
            propX(
                classX(make(FetchType)),
                constX(getAnnotationParameterValueOrDefault(annotation, "fetch", FetchType))
            )
        )

        otoAnnotation.addMember("mappedBy", constX(mappedBy))

        otoAnnotation.addMember("orphanRemoval", PRIM_TRUE)

        annotated.addAnnotation(otoAnnotation)

        if(getAnnotationParameterValueOrDefault(annotation, "createIfMissing", Boolean)) {
            String minorSetterName = GbtUtils.setterName(field)
            mainRecord.addObjectInitializerStatements(stmt(callThisX(minorSetterName, ctorX(subrecord))));
        }
    }

    private AnnotationNode reflexiveAnnotation(ClassNode mainRecord, ClassNode subrecord, AnnotationNode errorTarget) {
        List<AnnotationNode> subrecordOfAnnotations = subrecord.getAnnotations(SUBRECORD_OF_TYPE)
        if (subrecordOfAnnotations.empty) {
            addError("Subrecord class $subrecord.nameWithoutPackage is not annotated with @SubrecordOf", errorTarget)
            return null
        }

        List<ClassNode> mainRecordClasses = getAnnotationParameterClassesValue(subrecordOfAnnotations.get(0), "value")
        if (mainRecordClasses.empty || mainRecord != mainRecordClasses.get(0)) {
            addError("Subrecord class does not list this class as the main record", errorTarget)
            return null
        }

        return subrecordOfAnnotations.get(0)
    }
}
