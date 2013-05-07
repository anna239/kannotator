package org.jetbrains.kannotator.annotationsInference.immutability

enum class VariableImmutabilityAnnotation : Annotation {
    READ_ONLY
    IMMUTABLE
    MUTABLE
    ISOLATED
    AS_CLASS
    TRANSIENT
}

enum class MethodImmutabilityAnnotation: Annotation {
    CONST
    THIS_ISOLATED
}