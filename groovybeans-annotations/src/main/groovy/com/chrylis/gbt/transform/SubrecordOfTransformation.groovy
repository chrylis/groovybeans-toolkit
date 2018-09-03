package com.chrylis.gbt.transform;

import static com.chrylis.gbt.transform.GbtUtils.*
import static org.codehaus.groovy.ast.ClassHelper.make;

import javax.persistence.Entity
import javax.persistence.Id;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;

import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.transform.GroovyASTTransformation;

import com.chrylis.gbt.annotation.SubrecordOf
import com.chrylis.gbt.annotation.TwoWayRelationship

import groovy.transform.CompileStatic

@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
@CompileStatic
class SubrecordOfTransformation extends GbtTransformation<SubrecordOf> {

    @Override
    Class<SubrecordOf> annotationClass() { SubrecordOf }

    @Override
    public boolean canApplyToClassOnly() { return true }

    protected static final String ID_FIELD_PARAMETER_NAME = "idField"
    protected static final String MAIN_RECORD_FIELD_PARAMETER_NAME = "mainRecordField"

    protected static final ClassNode JPA_ENTITY_TYPE = make(Entity)

    protected static final ClassNode JPA_ID_TYPE = make(Id)
    protected static final ClassNode JPA_MAPSID_TYPE = make(MapsId)
    protected static final ClassNode JPA_ONETOONE_TYPE = make(OneToOne)

    protected static final ClassNode TWR_TYPE = make(TwoWayRelationship)

    @Override
    protected void doVisit(AnnotationNode annotationNode, AnnotatedNode annotatedNode) {
        ClassNode subRecordClass = (ClassNode) annotatedNode

        String idFieldName = getAnnotationParameterStringValue(annotationNode, ID_FIELD_PARAMETER_NAME)
        String mainRecordFieldName = getAnnotationParameterStringValue(annotationNode, MAIN_RECORD_FIELD_PARAMETER_NAME)

        // don't short-circuit; might as well return multiple errors if they're there
        if (fieldConflicts(subRecordClass, annotationNode, JPA_ID_TYPE, idFieldName)
            | fieldConflicts(subRecordClass, annotationNode, JPA_MAPSID_TYPE, mainRecordFieldName)) {
            return
        }

        // what's the main record?
        ClassNode mainRecordClass = mainRecordClassFrom(annotationNode);
        if (!mainRecordClass) {
            return
        }

        ClassNode idType = idTypeOf(mainRecordClass, annotationNode);
        if (!idType) {
            return
        }

        if(!subRecordClass.getAnnotations(JPA_ENTITY_TYPE)) {
            subRecordClass.addAnnotation(new AnnotationNode(JPA_ENTITY_TYPE))
        }

        addProperty(subRecordClass, idType, idFieldName).field.addAnnotation(new AnnotationNode(JPA_ID_TYPE))

        FieldNode mainRecordField = addProperty(subRecordClass, mainRecordClass, mainRecordFieldName).field

        AnnotationNode twrAnnotation = new AnnotationNode(TWR_TYPE);
        mainRecordField.addAnnotations([
            new AnnotationNode(JPA_ONETOONE_TYPE),
            new AnnotationNode(JPA_MAPSID_TYPE),
            twrAnnotation
        ])

        FieldNode correspondingFieldOnMainRecord = TwoWayRelationshipTransformation.findCorrespondingField(mainRecordField, sourceUnit)
        AnnotationNode correspondingAnnotation = correspondingFieldOnMainRecord.getAnnotations(TWR_TYPE).get(0);

        manuallyExecuteTransform(TwoWayRelationshipTransformation, twrAnnotation, mainRecordField, sourceUnit)
        manuallyExecuteTransform(TwoWayRelationshipTransformation, correspondingAnnotation, correspondingFieldOnMainRecord, sourceUnit)
    }

    protected boolean fieldConflicts(ClassNode cNode, AnnotationNode errorTarget, ClassNode annotationType, String fieldName) {
        if (cNode.getField(fieldName)) {
            addError("A field named $fieldName already exists.", errorTarget);
            return true
        }

        if (findAnnotatedMembers(cNode, annotationType)) {
            addError("A member annotated with @$annotationType.nameWithoutPackage already exists.", errorTarget)
            return true
        }

        return false
    }

    protected ClassNode mainRecordClassFrom(AnnotationNode annotation) {
        List<ClassNode> mainRecordClassParameter = getAnnotationParameterClassesValue(annotation, "value")
        if (mainRecordClassParameter.empty) {
            addError("$annotationName must specify the class of the main record.", annotation)
            return null
        }

        return mainRecordClassParameter.get(0)
    }

    protected ClassNode idTypeOf(ClassNode entityType, AnnotationNode errorTarget) {
        List<FieldNode> idFields = findAnnotatedFields(entityType, JPA_ID_TYPE)
        if (idFields.size() != 1) {
            addError(
                "Expected 1 @Id field on main record class $entityType.nameWithoutPackage but found ${idFields.size()}",
                errorTarget)
            return null
        }

        return idFields.get(0).type;
    }

    protected static PropertyNode addProperty(ClassNode target, ClassNode type, String name) {
        PropertyNode node = target.addProperty(name, ACC_PUBLIC, type, fieldDefaultValue(type), null, null)
        node.field.modifiers = ACC_PRIVATE;
        return node
    }
}
