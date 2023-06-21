package hk.ring0.othello.mcts

import hk.ring0.othello.game.Board
import hk.ring0.othello.game.Player
import hk.ring0.othello.game.Status

private const val UCT_C = 1.4142135623730951

fun uct(winScore: Int, visitCount: Int, totalVisit: Int): Double {
    if (visitCount == 0) {
        return Integer.MAX_VALUE.toDouble()
    }
    return (winScore.toDouble() / visitCount) + 
        UCT_C * Math.sqrt(Math.log(totalVisit.toDouble()) / visitCount)
}

class MonteCarloTreeSearch(val level: Int = 1) {

    private var opponent: Player = Player.BLACK

    fun findNextMove(board: Board, player: Player): Board? {
        opponent = player.opponent

        val rootNode = Node(State(board, opponent))
        val tree = Tree(rootNode)
        val end = System.nanoTime() + level * 1e8.toLong() // 100ms per level
        var simulations = 0
        val start = System.nanoTime()

        if (rootNode.state.getBoardStatus() != Status.ONGOING) {
            return null
        }

        while ((System.nanoTime() < end && simulations < 256)) {
            // println("\n----- Simulation $simulations -----\n")
            // selection
            val promisingNode = selectPromisingNode(rootNode)

            // expansion
            if (promisingNode.state.getBoardStatus() == Status.ONGOING) {
                expandNode(promisingNode)
            }

            // simulation
            var nodeToExplore = promisingNode
            if (promisingNode.childCount > 0) {
                nodeToExplore = promisingNode.randomChild
            }
            val result = simulateRandomPlayout(nodeToExplore)
            // println("Simulation $simulations result: $result")

            // backpropagation
            backPropagation(nodeToExplore, result)
            simulations++
        }

        val elapsed = (System.nanoTime() - start) / 1e6
        val (treeSize, treeHeight) = tree.dimension
        println("simulations: $simulations tree: [size=${treeSize} height=${treeHeight}] time: $elapsed ms")
        return rootNode.bestChild.state.board
    }

    private fun selectPromisingNode(rootNode: Node): Node {
        var node = rootNode
        while (node.childCount != 0) {
            node = node.findBestNodeWithUCT()
        }
        return node
    }
    
    private fun expandNode(node: Node) {
        for (state in node.state.findPossibleStates()) {
            node += Node(state)
        }
    }

    private fun simulateRandomPlayout(node: Node): Status {
        var status = node.state.getBoardStatus()
        if (status.ordinal == opponent.id) {
            node.parent!!.state.winScore = Int.MIN_VALUE
            // println("Skipping simulation on losing node")
            return status
        }
        val tempState = node.state.deepCopy()
        var turn = 1
        while (status == Status.ONGOING) {
            tempState.switchSide()
            // val move = tempState.playRandom()
            // if (move != null) {
            //     val (x, y) = move
            //     println("Simulation turn N+$turn: ${tempState.player} plays $x, $y")
            //     // println(tempState.board.stringify(x, y))
            // } else {
            //     println("Simulation turn N+$turn: ${tempState.player} passes")
            // }
            tempState.playRandom()
            status = tempState.getBoardStatus()
            turn++
        }
        return status
    }

    private fun backPropagation(nodeToExplore: Node, result: Status) {
        var node: Node? = nodeToExplore
        while (node != null) {
            node.state.visitCount++
            if (node.state.player.id == result.ordinal) {
                node.state.addScore(10)
            }
            node = node.parent
        }
    }
}
