package com.chrylis.gbt.transform

import static com.chrylis.gbt.transform.GbtUtils.getAnnotationParameterStringValue
import static groovyjarjarasm.asm.Opcodes.*
import static org.codehaus.groovy.ast.ClassHelper.isPrimitiveType
import static org.codehaus.groovy.ast.tools.GeneralUtils.*

import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.PropertyNode
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.GroovyASTTransformation

import com.chrylis.gbt.annotation.TwoWayRelationship

import groovy.transform.CompileStatic
import groovy.transform.PackageScope

@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
@CompileStatic
class TwoWayRelationshipTransformation extends GbtTransformation<TwoWayRelationship> {

    @Override
    Class<TwoWayRelationship> annotationClass() { TwoWayRelationship }

    @Override
    boolean canApplyToClassOnly() { false }

    protected final ClassNode MY_TYPE = annotationClassNode();

    /**
     * Validates the structure of the bidirectional relationship and creates the code necessary for
     * setting the relationship from this side.
     *
     * The algorithm implemented here is:
     *
     * <ol>
     * <li>Sanity-check the annotated field (it backs a mutable reference property).
     * <li>Find the corresponding field representing the inverse side of the relationship.
     * <li>Confirm that the corresponding field points back here.
     * <li>Add a synthetic setter to be used by the corresponding class to avoid a setter loop.
     * <li>Create a public setter for this field's property that sets both sides of the relationship,
     * including clearing dangling relationships if any.
     * </ol>
     *
     * Note in particular that this transformation apples only to this side of the relationship,
     * but it is required that the same transformation be applied to the other side independently.
     */
    @Override
    protected void doVisit(AnnotationNode annotation, AnnotatedNode annotated) {
        FieldNode myField = (FieldNode) annotated
        if (!sane(myField)) {
            return
        }

        FieldNode correspondingField = findCorrespondingField(myField)
        if (!correspondingField) {
            return
        }

        FieldNode roundTrip = findCorrespondingField(correspondingField)
        if(roundTrip != myField) {
            StringBuilder sb = new StringBuilder("corresponding field ")
                .append(myField.type.nameWithoutPackage).append('#').append(correspondingField.name)

            if(roundTrip) {
                sb.append(" is already mapped to a different relationship (")
                    .append(roundTrip.name).append("); the other field may need an explicit mapping")
            } else {
                sb.append(' does not map to a field on this class')
            }

            addError(sb.toString(), myField)
            return
        }

        addGeneratedCode(myField, correspondingField)
    }

    protected static final int INTERESTING_MODIFIERS = ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED | ACC_STATIC | ACC_FINAL;
    protected static final int EXPECTED_MODIFIERS = ACC_PRIVATE;

    /**
     * Confirms that the transformation is being applied to a mutable reference property.
     *
     * @param field the field to inspect
     * @return {@code false} if the field is final or primitive, or {@code true} otherwise
     */
    protected boolean sane(FieldNode field) {
        if ((field.modifiers & INTERESTING_MODIFIERS) != EXPECTED_MODIFIERS ||
            field.declaringClass.getProperty(field.name) == null) {

            addError("${annotationName} must be applied to a mutable Groovy property", field)
            return false
        }

        if(isPrimitiveType(field.type)) {
            addError("${annotationName} makes no sense on a primitive property", field)
            return false
        }

        return true
    }

    /**
     * Finds the "corresponding field" that is the other end of this field's bidirectional relationship.
     *
     * This method checks to see whether the annotation on the field, which must be present, specifies
     * a {@code mappedBy} parameter; if so, then it returns the specified field. If not, it examines
     * all the fields on the corresponding class, returning a single matching field and raising an error
     * if zero or multiple fields match.
     *
     * @param field
     *            the field whose correspondence is to be checked
     * @return the field on the other end of the bidirectional relationship, or {@code null} if no single matching field could be
     *         identified
     */
    protected FieldNode findCorrespondingField(FieldNode field) {
        // disregard entirely if this field is not annotated correctly, since we need to inspect the annotation
        List<AnnotationNode> annotations = field.getAnnotations(MY_TYPE)
        if (annotations.empty) {
            return null
        }

        ClassNode correspondingClass = field.type

        // if the annotation specifies a "mappedBy" field, examine exactly that field
        String explicitFieldName = getAnnotationParameterStringValue(annotations.get(0), "mappedBy")
        if (!explicitFieldName.empty) {
            FieldNode specified = correspondingClass.getField(explicitFieldName);
            if (specified == null) {
                addError("the specified field $explicitFieldName was not found on $correspondingClass", field)
            } else if (specified.getAnnotations(MY_TYPE).empty) {
                addError("the corresponding field $correspondingClass.nameWithoutPackage#$specified.name is not annotated with $annotationName", field)
                specified = null
            }
            return specified
        }

        // if no field is specified, find all annotated fields on the corresponding class of the owning type
        ClassNode thisClass = field.declaringClass
        List<FieldNode> candidates = correspondingClass.fields
            .findAll { it.type == thisClass }
            .findAll { it.getAnnotations(MY_TYPE) }

        // only successful path for unspecified corresponding field
        if (candidates.size() == 1) {
            return candidates.get(0)
        }

        StringBuilder sb = new StringBuilder("no explicit field name was provided and ")
        if (candidates.size() == 0) {
            sb.append("no matching fields were found")
        } else {
            sb.append(candidates.size()).append(" matching fields were found: ")
            sb.append(candidates*.name)
        }
        addError(sb.toString(), field)

        return null
    }

    /**
     * Adds the generated code to this class necessary to implement its half of the two-way relationship.
     * This method is called as part of the transformation but is also available to other transformations
     * that are generating corresponding properties on the fly and need to bypass the normal sanity
     * checks.
     *
     * @param thisSide the field on this class representing our side of the relationship
     * @param otherSide the corresponding field on the other class
     */
    protected static void addGeneratedCode(FieldNode thisSide, FieldNode otherSide) {
        addSyntheticSetterFor(thisSide)

        PropertyNode property = thisSide.declaringClass.getProperty(thisSide.name)
        property.setterBlock = managedSetterBody(thisSide, otherSide)
    }

    protected static void addSyntheticSetterFor(FieldNode field) {
        field.declaringClass.addMethod(
            syntheticSetterName(field),
            ACC_PUBLIC | ACC_FINAL | ACC_SYNTHETIC,
            ClassHelper.VOID_TYPE,
            [setterParam(field)] as Parameter[],
            [] as ClassNode[],
            syntheticSetterBody(field)
        )
    }

    protected static String syntheticSetterName(FieldNode field) {
        '$gbt_' + GbtUtils.setterName(field)
    }

    protected static Parameter setterParam(FieldNode field) {
        param(field.type, field.name)
    }

    protected static Statement syntheticSetterBody(FieldNode field) {
        return assignS(fieldX(field), varX(setterParam(field)));
    }

    protected static Statement managedSetterBody(FieldNode myField, FieldNode corresponding) {

        // The field on this class representing the relationship.
        Expression owningField = fieldX(myField)

        // The setter parameter (hard-set by Groovy as "value").
        Expression newCorrespondingObject = varX(param(myField.type, "value"))

        // Early exit if the parameter is already associated with this instance.
        Statement earlyExit = ifS(sameX(owningField, newCorrespondingObject), ReturnStatement.RETURN_NULL_OR_VOID);

        // If this object already has a relationship, null out the other side.
        Statement breakUpWithEx = ifS(
            notNullX(owningField),
            stmt(callX(owningField, syntheticSetterName(corresponding), ConstantExpression.NULL))
        )

        // Set this end of the relationship. There might not be another end.
        Statement iTakeYou = assignS(owningField, newCorrespondingObject)
        Statement exitIfNowSingle = (ifS(equalsNullX(newCorrespondingObject), ReturnStatement.RETURN_NULL_OR_VOID))

        // See if the new corresponding object already had a relationship. If so, null out the other end of that one.
        VariableExpression jilted = varX("jilted", corresponding.type)
        Statement findJilted = declS(jilted, propX(newCorrespondingObject, corresponding.name))
        Statement stealSignificantOther = ifS(
            notNullX(jilted),
            assignS(attrX(jilted, constX(myField.name)), constX(null))
        )

        // Set both ends of the new relationship.
        Statement youTakeMe = stmt(callX(newCorrespondingObject, syntheticSetterName(corresponding), varX("this")))

        return block(earlyExit, breakUpWithEx, iTakeYou, exitIfNowSingle, findJilted, stealSignificantOther, youTakeMe)
    }

    static FieldNode findCorrespondingField(FieldNode source, SourceUnit sourceUnit) {
        new TwoWayRelationshipTransformation(sourceUnit: sourceUnit).findCorrespondingField(source)
    }
}
