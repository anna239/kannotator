package org.jetbrains.kannotator.annotationsInference.immutability

import org.jetbrains.kannotator.runtime.annotations.AnalysisType
import org.jetbrains.kannotator.controlFlow.builder.analysis.Qualifier
import org.jetbrains.kannotator.controlFlow.builder.analysis.QualifierSet

class ImmutabilityKey: Any(), AnalysisType {
    public override fun toString(): String = "immutability"
}

val IMMUTABILITY_KEY = ImmutabilityKey()


public enum class Immutability(val lower: VariableImmutabilityAnnotation, val upper: VariableImmutabilityAnnotation): Qualifier {
    READ_ONLY: Immutability(VariableImmutabilityAnnotation.READ_ONLY, VariableImmutabilityAnnotation.READ_ONLY)
    IMMUTABLE: Immutability(VariableImmutabilityAnnotation.IMMUTABLE, VariableImmutabilityAnnotation.IMMUTABLE)
    MUTABLE: Immutability(VariableImmutabilityAnnotation.MUTABLE, VariableImmutabilityAnnotation.MUTABLE)
    ISOLATED: Immutability(VariableImmutabilityAnnotation.ISOLATED, VariableImmutabilityAnnotation.ISOLATED)

    READ_ONLY_2_IMMUTABLE: Immutability(VariableImmutabilityAnnotation.READ_ONLY, VariableImmutabilityAnnotation.IMMUTABLE)
    READ_ONLY_2_ISOLATED: Immutability(VariableImmutabilityAnnotation.READ_ONLY, VariableImmutabilityAnnotation.ISOLATED)
    MUTABLE_2_ISOLATED: Immutability(VariableImmutabilityAnnotation.MUTABLE, VariableImmutabilityAnnotation.ISOLATED)
    IMMUTABLE_2_ISOLATED: Immutability(VariableImmutabilityAnnotation.IMMUTABLE, VariableImmutabilityAnnotation.ISOLATED)
}

fun getPreciseImmutabilityValue(v: VariableImmutabilityAnnotation): Immutability {
    return getImmutabilityValue(v, v)
}

fun getImmutabilityValue(lower: VariableImmutabilityAnnotation, upper: VariableImmutabilityAnnotation): Immutability {
    return Immutability.values().filter { it.upper == upper && it.lower == lower }.first()
}


public object ImmutabilitySet: QualifierSet<Immutability> {

    public override val id: AnalysisType = IMMUTABILITY_KEY
    public override val initial: Immutability = Immutability.READ_ONLY_2_ISOLATED
    public override fun merge(q1: Immutability, q2: Immutability): Immutability {
        if (q1 == q2) {
            return q1;
        }
        if (q1 == Immutability.READ_ONLY_2_ISOLATED) {
            return q2;
        }
        if (q2 == Immutability.READ_ONLY_2_ISOLATED) {
            return q2;
        }
        if (q1 == Immutability.READ_ONLY || q2 == Immutability.READ_ONLY) {
            return Immutability.READ_ONLY;
        }
        if (q1.lower == q2.upper) {
            return getPreciseImmutabilityValue(q1.lower);
        }
        if (q2.lower == q1.upper) {
            return getPreciseImmutabilityValue(q1.upper);
        }
        if (q1 == Immutability.ISOLATED && q2.lower == VariableImmutabilityAnnotation.ISOLATED) {
            return Immutability.ISOLATED;
        }
        if (q2 == Immutability.ISOLATED && q1.lower == VariableImmutabilityAnnotation.ISOLATED) {
            return Immutability.ISOLATED;
        }
        return Immutability.READ_ONLY;
    }
    public override fun contains(q: Qualifier): Boolean = q is Immutability
}


