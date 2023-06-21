package hk.ring0.othello.mcts

import kotlin.random.Random

import hk.ring0.othello.game.Board
import hk.ring0.othello.game.Player

data class State(
    val board: Board,
    var player: Player = Player.BLACK,
) {
    var visitCount = 0
    var winScore = 0

    fun deepCopy(): State {
        return this.copy(board = board.copy())
    }

    fun getBoardStatus() = board.getStatus(player.opponent)

    fun findPossibleStates(): List<State> {
        val states = board.findPossibleStates(player.opponent)
        return states.map { State(it, player.opponent) }
    }

    fun addScore(score: Int) {
        if (winScore != Int.MIN_VALUE) {
            winScore += score
        }
    }

    fun switchSide() {
        player = player.opponent
    }

    fun playRandom(): IntArray? {
        val moves = board.findPossibleMoves(player)
        if (moves.size > 0) {
            val i = Random.nextInt(moves.size)
            board.play(moves[i], player)
            return intArrayOf(moves[i].x, moves[i].y)
        } else {
            board.pass()
            return null
        }
    }
}
