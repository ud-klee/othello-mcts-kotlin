package hk.ring0.othello.mcts

import kotlin.random.Random

import hk.ring0.othello.mcts.uct

typealias NodeVisitor = (node: Node, height: Int) -> Unit

data class Node(
    val state: State,
) {
    var parent: Node? = null
        private set

    private val children = mutableListOf<Node>()

    val childCount get() = children.size

    val randomChild get() = children[Random.nextInt(children.size)]

    val bestChild get() = children.maxBy { it.state.visitCount }

    operator fun plusAssign(child: Node) {
        child.parent = this
        children.add(child)
    }

    fun apply(height: Int = 1, fn: NodeVisitor) {
        fn(this, height)
        for (child in children) {
            child.apply(height + 1, fn)
        }
    }

    fun findBestNodeWithUCT(): Node {
        val parentVisit = state.visitCount
        return children.maxBy { uct(it.state.winScore, it.state.visitCount, parentVisit) }
    }
    
}
