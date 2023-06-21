package hk.ring0.othello.mcts

data class Tree(val root: Node) {

    val dimension: IntArray get() {
        var size = 0
        var height = 0
        apply { _, h ->
            size++
            height = maxOf(height, h)
        }
        return intArrayOf(size, height)
    }

    fun apply(fn: NodeVisitor) {
        root.apply(1, fn)
    }

}
