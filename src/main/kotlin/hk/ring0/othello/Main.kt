package hk.ring0.othello

import java.io.File

import com.google.gson.Gson

import hk.ring0.othello.game.Board
import hk.ring0.othello.game.Player
import hk.ring0.othello.game.Status
import hk.ring0.othello.mcts.MonteCarloTreeSearch

fun replay(path: String) {
    val s = File(path).readText()
    val gson = Gson()
    val history = gson.fromJson(s, Array<IntArray?>::class.java)
    val board = Board()
    var player = Player.BLACK
    var turn = 1

    for (move in history) {
        if (move != null) {
            val (x, y) = move
            println("Turn $turn: $player plays $x, $y")
            board.play(x, y, player)
            println(board.stringify(x, y))

            val (black, white) = board.getScores()
            println("Black: $black, White: $white")
        } else {
            println("Turn $turn: $player passes")
            board.pass()
        }
        player = player.opponent
        turn++
    }

    val status = board.getStatus(player)
    println("Result: $status")
}

fun simulate() {
    var board = Board()
    var player = Player.BLACK
    var turn = 1
    var status = Status.ONGOING

    val mcts = MonteCarloTreeSearch(level = 2)

    var totalMem = Runtime.getRuntime().totalMemory() / 1024
    var freeMem = Runtime.getRuntime().freeMemory() / 1024
    println("Total mem: $totalMem | Free mem: $freeMem | Used: ${totalMem - freeMem}")

    while (status == Status.ONGOING) {
        println("Turn $turn")

        val tempBoard = mcts.findNextMove(board, player)
        if (tempBoard == null) {
            println("No more moves!")
            break
        }

        board = tempBoard
        // println(board)

        player = player.opponent
        status = board.getStatus(player)
        val (blacks, whites) = board.getScores()
        println("Scores(B/W): $blacks/$whites, Status: $status")

        turn++
    }

    totalMem = Runtime.getRuntime().totalMemory() / 1024
    freeMem = Runtime.getRuntime().freeMemory() / 1024
    println("Total mem: $totalMem | Free mem: $freeMem | Used: ${totalMem - freeMem}")
}

fun main(args: Array<String>) {
    if (args.size > 0 && args[0] == "replay") {
        replay(args[1])
    } else {
        simulate()
    }
}
