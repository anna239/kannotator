package org.jetbrains.kannotator.annotationsInference.immutability

import org.jetbrains.kannotator.runtime.annotations.AnalysisType
import org.jetbrains.kannotator.controlFlow.builder.analysis.Qualifier
import org.jetbrains.kannotator.controlFlow.builder.analysis.QualifierSet

class ImmutabilityKey: Any(), AnalysisType {
    public override fun toString(): String = "immutability"
}

val IMMUTABILITY_KEY = ImmutabilityKey()

public trait Immutability {
    public fun getValue(): ImmutabilityValue
    public fun setLower(lower: VariableImmutabilityAnnotation)
    public fun setUpper(upper: VariableImmutabilityAnnotation)
}

public class SimpleImmutability(private var value: ImmutabilityValue): Immutability {

    public override fun getValue(): ImmutabilityValue {
        return value;
    }

    public override fun setLower(lower: VariableImmutabilityAnnotation) {
        value = getImmutabilityValue(value.upper, lower)
    }
    public override fun setUpper(upper: VariableImmutabilityAnnotation) {
        value = getImmutabilityValue(upper, value.lower)
    }
}

public class ComplexImmutability(val left: Immutability, val right: Immutability): Immutability {

    public override fun getValue(): ImmutabilityValue {
        return merge(left.getValue(), right.getValue())
    }

    public override fun setLower(lower: VariableImmutabilityAnnotation) {
        left.setLower(lower)
        right.setLower(lower)
    }
    public override fun setUpper(upper: VariableImmutabilityAnnotation) {
        left.setUpper(upper)
        right.setUpper(upper)
    }
}

public enum class ImmutabilityValue(val lower: VariableImmutabilityAnnotation, val upper: VariableImmutabilityAnnotation) {
    READ_ONLY: ImmutabilityValue(VariableImmutabilityAnnotation.READ_ONLY, VariableImmutabilityAnnotation.READ_ONLY)
    IMMUTABLE: ImmutabilityValue(VariableImmutabilityAnnotation.IMMUTABLE, VariableImmutabilityAnnotation.IMMUTABLE)
    MUTABLE: ImmutabilityValue(VariableImmutabilityAnnotation.MUTABLE, VariableImmutabilityAnnotation.MUTABLE)
    ISOLATED: ImmutabilityValue(VariableImmutabilityAnnotation.ISOLATED, VariableImmutabilityAnnotation.ISOLATED)

    READ_ONLY_2_IMMUTABLE: ImmutabilityValue(VariableImmutabilityAnnotation.READ_ONLY, VariableImmutabilityAnnotation.IMMUTABLE)
    READ_ONLY_2_ISOLATED: ImmutabilityValue(VariableImmutabilityAnnotation.READ_ONLY, VariableImmutabilityAnnotation.ISOLATED)
    MUTABLE_2_ISOLATED: ImmutabilityValue(VariableImmutabilityAnnotation.MUTABLE, VariableImmutabilityAnnotation.ISOLATED)
    IMMUTABLE_2_ISOLATED: ImmutabilityValue(VariableImmutabilityAnnotation.IMMUTABLE, VariableImmutabilityAnnotation.ISOLATED)
}

fun getPreciseImmutabilityValue(v: VariableImmutabilityAnnotation): ImmutabilityValue {
    getImmutabilityValue(v, v)
}

fun getImmutabilityValue(lower: VariableImmutabilityAnnotation, upper: VariableImmutabilityAnnotation): ImmutabilityValue {
    return ImmutabilityValue.values().filter { it.upper == upper && it.lower == lower }.first()
}

fun merge(q1: ImmutabilityValue, q2: ImmutabilityValue): ImmutabilityValue {
    if (q1 == q2) {
        return q1;
    }
    if (q1 == ImmutabilityValue.READ_ONLY_2_ISOLATED) {
        return q2;
    }
    if (q2 == ImmutabilityValue.READ_ONLY_2_ISOLATED) {
        return q2;
    }
    if (q1 == ImmutabilityValue.READ_ONLY || q2 == ImmutabilityValue.READ_ONLY) {
        return ImmutabilityValue.READ_ONLY;
    }
    if (q1.lower == q2.upper) {
        return getPreciseImmutabilityValue(q1.lower);
    }
    if (q2.lower == q1.upper) {
        return getPreciseImmutabilityValue(q1.upper);
    }
    if (q1 == ImmutabilityValue.ISOLATED && q2.lower == VariableImmutabilityAnnotation.ISOLATED) {
        return ImmutabilityValue.ISOLATED;
    }
    if (q2 == ImmutabilityValue.ISOLATED && q1.lower == VariableImmutabilityAnnotation.ISOLATED) {
        return ImmutabilityValue.ISOLATED;
    }
    return ImmutabilityValue.READ_ONLY;
}


public object ImmutabilitySet: QualifierSet<Immutability> {

    public override val id: AnalysisType = IMMUTABILITY_KEY
    public override val initial: Immutability = SimpleImmutability(ImmutabilityValue.READ_ONLY_2_ISOLATED)
    public override fun merge(q1: Immutability, q2: Immutability): Immutability {
        return ComplexImmutability(q1, q2);
    }
    public override fun contains(q: Qualifier): Boolean = q is Immutability
}


