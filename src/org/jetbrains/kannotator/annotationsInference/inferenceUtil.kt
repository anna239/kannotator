package org.jetbrains.kannotator.annotationsInference

import kotlinlib.emptySet
import org.jetbrains.kannotator.asm.util.getArgumentCount
import org.jetbrains.kannotator.asm.util.getAsmInstructionNode
import org.jetbrains.kannotator.asm.util.getOpcode
import org.jetbrains.kannotator.controlFlow.ControlFlowGraph
import org.jetbrains.kannotator.controlFlow.Instruction
import org.jetbrains.kannotator.controlFlow.Value
import org.jetbrains.kannotator.controlFlow.builder.AsmInstructionMetadata
import org.jetbrains.kannotator.controlFlow.builder.STATE_BEFORE
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.declarations.PositionsForMethod
import org.jetbrains.kannotator.index.DeclarationIndex
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.FieldInsnNode
import org.jetbrains.kannotator.declarations.Field

trait Annotation

fun ControlFlowGraph.traverseInstructions(f: (Instruction) -> Unit) {
    for (instruction in instructions) {
        if (instruction[STATE_BEFORE] == null) continue // dead instructions
        f(instruction)
    }
}

public fun <A: Annotation> generateAssertsForCallArguments(
        instruction: Instruction,
        declarationIndex: DeclarationIndex,
        annotations: Annotations<A>,
        addAssertForStackValue: (Int) -> Unit,
        needGenerateAssertForThis: Boolean,
        needGenerateAssertForArgument: (A) -> Boolean
) {
    val instructionNode = instruction.getAsmInstructionNode()
    if (instructionNode !is MethodInsnNode) throw IllegalArgumentException("Not a method instruction: $instruction")
    val hasThis = instruction.getOpcode() != INVOKESTATIC
    val thisSlots = if (hasThis) 1 else 0
    val parametersCount = instructionNode.getArgumentCount() + thisSlots

    fun addAssertForArgumentOnStack(index: Int) {
        addAssertForStackValue(parametersCount - index - 1)
    }

    if (hasThis && needGenerateAssertForThis) {
        addAssertForArgumentOnStack(0)
    }
    if (instruction.getOpcode() != INVOKEDYNAMIC) {
        val method = declarationIndex.findMethodByInstruction(instruction)
        if (method != null) {
            val positions = PositionsForMethod(method)
            for (paramIndex in thisSlots..parametersCount - 1) {
                val paramAnnotation = annotations[positions.forParameter(paramIndex).position]
                if (paramAnnotation != null && needGenerateAssertForArgument(paramAnnotation)) {
                    addAssertForArgumentOnStack(paramIndex)
                }
            }
        }
    }
}

fun DeclarationIndex.findMethodByInstruction(instruction: Instruction): Method? {
    val methodInsnNode = (instruction.metadata as? AsmInstructionMetadata)?.asmInstruction as? MethodInsnNode
    return if (methodInsnNode == null) null else this.findMethodByMethodInsnNode(methodInsnNode)
}

fun DeclarationIndex.findMethodByMethodInsnNode(methodInsnNode: MethodInsnNode): Method? {
    return this.findMethod(ClassName.fromInternalName(methodInsnNode.owner!!), methodInsnNode.name!!, methodInsnNode.desc)
}

fun DeclarationIndex.findFieldByFieldInsnNode(fieldInsnNode: FieldInsnNode): Field? {
    return this.findField(ClassName.fromInternalName(fieldInsnNode.owner), fieldInsnNode.name)
}

fun Instruction.getReceiverValues(): Set<Value> {
    val state = this[STATE_BEFORE]!!
    val asmInstruction = (metadata as? AsmInstructionMetadata)?.asmInstruction
    if (asmInstruction !is MethodInsnNode) throw IllegalArgumentException("Not a method instruction: $asmInstruction")
    return state.stack[asmInstruction.getArgumentCount()]
}

fun PositionsForMethod.forInterestingValue(value: Value) = forParameter(value.parameterIndex!!).position
