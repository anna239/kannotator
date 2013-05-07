package org.jetbrains.kannotator.annotationsInference.immutability

import org.jetbrains.kannotator.controlFlow.builder.analysis.Qualifier
import org.jetbrains.kannotator.index.DeclarationIndex
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.controlFlow.builder.analysis.BasicFrameTransformer
import org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.kannotator.controlFlow.builder.analysis.QualifiedValueSet
import org.objectweb.asm.tree.analysis.Frame
import org.jetbrains.kannotator.annotationsInference.engine.Analyzer
import org.jetbrains.kannotator.annotationsInference.engine.ResultFrame
import org.jetbrains.kannotator.annotationsInference.engine.EdgeKind
import org.objectweb.asm.Opcodes.*
import org.jetbrains.kannotator.declarations.Method
import org.objectweb.asm.tree.MethodNode
import org.jetbrains.kannotator.annotationsInference.engine.AnalysisResult
import org.jetbrains.kannotator.index.FieldDependencyInfo
import org.jetbrains.kannotator.declarations.Field
import org.jetbrains.kannotator.declarations.PositionsForMethod
import org.objectweb.asm.Type
import org.jetbrains.kannotator.asm.util.isPrimitiveOrVoidType
import org.jetbrains.kannotator.declarations.AnnotationsImpl
import org.jetbrains.kannotator.declarations.setIfNotNull
import org.jetbrains.kannotator.annotationsInference.immutability.Immutability.*
import org.jetbrains.kannotator.controlFlow.builder.analysis.getStackFromTop
import org.jetbrains.kannotator.annotationsInference.interpreter.*
import org.jetbrains.kannotator.declarations.getFieldTypePosition
import java.util.Collections
import java.util.HashMap

class ImmutabilityFrameTransformer<T: Qualifier>(val annotations: Annotations<VariableImmutabilityAnnotation>,
                                                 val declarationIndex: DeclarationIndex): BasicFrameTransformer<T>() {

    public override fun getPseudoResults(insnNode: AbstractInsnNode,
                                         preFrame: Frame<QualifiedValueSet<T>>,
                                         executedFrame: Frame<QualifiedValueSet<T>>,
                                         analyzer: Analyzer<QualifiedValueSet<T>>): Collection<ResultFrame<QualifiedValueSet<T>>> {
        return super<BasicFrameTransformer>.getPseudoResults(insnNode, preFrame, executedFrame, analyzer)
    }
    public override fun getPostFrame(insnNode: AbstractInsnNode,
                                     edgeKind: EdgeKind,
                                     preFrame: Frame<QualifiedValueSet<T>>,
                                     executedFrame: Frame<QualifiedValueSet<T>>,
                                     analyzer: Analyzer<QualifiedValueSet<T>>): Frame<QualifiedValueSet<T>>? {
        val defFrame = super<BasicFrameTransformer>.getPostFrame(insnNode, edgeKind, preFrame, executedFrame, analyzer)
        return defFrame
    }
}

class AnnotationsBuildingResult(val inferredAnnotations: Annotations<Immutability>,
                                val transientFieldsImmutability: Map<Field, Immutability>)

fun <T: Qualifier> buildMethodImmutabilityAnnotations(method: Method,
                                       methodNode: MethodNode,
                                       analysisResult: AnalysisResult<QualifiedValueSet<T>>,
                                       fieldDependencyInfoProvider: (Field) -> FieldDependencyInfo,
                                       declarationIndex: DeclarationIndex,
                                       annotations: Annotations<VariableImmutabilityAnnotation>,
                                       methodFieldsImmutabilityInfoProvider: (Method) -> Map<Field, Immutability>?):
        AnnotationsBuildingResult {
    val positions = PositionsForMethod(method)
    val inferredAnnotations = AnnotationsImpl<Immutability>()

    fun collectReturnValueImmutability(): Immutability {
        var returnValueInfo = READ_ONLY_2_ISOLATED
        for (returnInsn in analysisResult.returnInstructions) {
            val resultFrame = analysisResult.mergedFrames[returnInsn]!!
            if (returnInsn.getOpcode() == ARETURN) {
                val returnValues: QualifiedValueSet<T>? = resultFrame.getStackFromTop(0)
                if (returnValues != null) {
                    returnValueInfo = returnValues.values.fold(returnValueInfo,
                            {(q, v) -> ImmutabilitySet.merge(q, v.qualifier.extract<Immutability>(ImmutabilitySet))})
                }
            }
        }
        return returnValueInfo
    }

    fun buildMergedParameterMap(insnSet: Set<AbstractInsnNode>): Map<Int, Immutability> {
        if (insnSet.isEmpty()) {
            return Collections.emptyMap()
        }
        val paramInfoMap = HashMap<Int, Immutability>()
        for (insn in insnSet) {
            val frame = analysisResult.mergedFrames[insn]!!
            val localParamInfoMap = HashMap<Int, Immutability>()
        }
    }

    fun collectFieldImmutability(): Map<Field, Immutability> = null

    fun findFieldsWithChangedImmutabilityInfo(prev: Map<Field, Immutability>?,
                                              curr: Map<Field, Immutability>): Collection<Field> = null

    fun buildFieldImmutability(fieldInfo: FieldDependencyInfo,
                               methodToFieldsImmutabilityProvider: (Method) -> Map<Field, Immutability>?): Immutability? = null

    if (!Type.getReturnType(methodNode.desc).isPrimitiveOrVoidType()) {
        inferredAnnotations.setIfNotNull(positions.forReturnType().position, collectReturnValueImmutability())
    }
    val paramInfoMap = buildMergedParameterMap(analysisResult.returnInstructions)
    for ((paramIdx, paramInfo) in paramInfoMap) {
        val pos = positions.forParameter(paramIdx).position
        inferredAnnotations.setIfNotNull(pos, paramInfo)
    }

    val fieldInfoMap = collectFieldImmutability()
    val updatedFieldInfoProvider = {(m: Method) -> if (m == method) fieldInfoMap else methodFieldsImmutabilityInfoProvider(m)}
    for (changedField in findFieldsWithChangedImmutabilityInfo(methodFieldsImmutabilityInfoProvider(method), fieldInfoMap)) {
        val fieldImmutability = buildFieldImmutability(fieldDependencyInfoProvider(changedField), updatedFieldInfoProvider)
        if (fieldImmutability != null) {
            inferredAnnotations[getFieldTypePosition(changedField)] = fieldImmutability
        }
    }

    return AnnotationsBuildingResult(inferredAnnotations, fieldInfoMap)
}

