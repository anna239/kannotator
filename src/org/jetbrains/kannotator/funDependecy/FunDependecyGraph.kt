package org.jetbrains.kannotator.funDependecy

import java.util.ArrayList
import org.jetbrains.kannotator.declarations.Method
import java.util.LinkedHashSet
import java.util.LinkedHashMap

public trait FunDependencyGraph {
    val functions: List<FunctionNode>
    val noOutgoingNodes: List<FunctionNode>
}

public trait FunDependencyEdge {
    val from: FunctionNode
    val to: FunctionNode
}

public trait FunctionNode {
    val incomingEdges: Collection<FunDependencyEdge>
    val outgoingEdges: Collection<FunDependencyEdge>
    val method: Method
}

class FunDependencyGraphImpl : FunDependencyGraph {
    override val noOutgoingNodes: List<FunctionNode> get() = noOutgoingNodesSet.toList()
    override val functions: List<FunctionNode> get() = nodes.values().toList()

    private val noOutgoingNodesSet = LinkedHashSet<FunctionNode>()
    private val nodes = LinkedHashMap<Method, FunctionNodeImpl>()

    fun getOrCreateNode(method : Method) : FunctionNodeImpl {
        return nodes.getOrPut(method, {
            val funNode = FunctionNodeImpl(method)
            noOutgoingNodesSet.add(funNode)
            funNode
        })
    }

    fun createEdge(from: FunctionNodeImpl, to: FunctionNodeImpl, debugName: String? = null) : FunDependencyEdgeImpl {
        val edge = FunDependencyEdgeImpl(from, to, debugName)

        from.outgoingEdges.add(edge)
        to.incomingEdges.add(edge)

        noOutgoingNodesSet.remove(from)

        return edge
    }
}

class FunDependencyEdgeImpl(override val from: FunctionNode,
                            override val to: FunctionNode,
                            val debugName: String? = null): FunDependencyEdge {

    fun toString(): String {
        val prefix = if (debugName != null) debugName + " " else ""
        return "${prefix}${from.method} -> ${to.method}"
    }

    fun hashCode(): Int {
        return from.hashCode() * 31 + to.hashCode()
    }

    public fun equals(obj: Any?): Boolean {
        if (obj is FunDependencyEdge) {
            return from == obj.from && to == obj.to
        }
        return false
    }
}

class FunctionNodeImpl(override val method: Method): FunctionNode {
    override val outgoingEdges: MutableCollection<FunDependencyEdge> = LinkedHashSet()
    override val incomingEdges: MutableCollection<FunDependencyEdge> = LinkedHashSet()

    fun toString(): String {
        return "${method} in$incomingEdges out$outgoingEdges"
    }
}