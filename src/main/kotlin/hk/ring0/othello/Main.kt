package hk.ring0.othello

import java.io.File

import com.google.gson.Gson

import hk.ring0.othello.game.Board
import hk.ring0.othello.game.BoardCache
import hk.ring0.othello.game.BoardStorage
import hk.ring0.othello.game.Player
import hk.ring0.othello.game.Status
import hk.ring0.othello.mcts.MonteCarloTreeSearch

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.response.*
import io.ktor.server.routing.*

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
            println(board)

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

    val mcts = MonteCarloTreeSearch(level = 4)

    var totalMem = Runtime.getRuntime().totalMemory() / 1024
    var freeMem = Runtime.getRuntime().freeMemory() / 1024
    println("Memory (totalKB/usedKB): $totalMem/${totalMem - freeMem}")

    while (status == Status.ONGOING) {
        println("Turn $turn: $player")

        val tempBoard = mcts.findNextMove(board, player)
        if (tempBoard == null) {
            println("No more moves!")
            break
        }

        board = tempBoard
        println(board)
        println(board.toHex())

        player = player.opponent
        status = board.getStatus(player)
        val (blacks, whites) = board.getScores()
        println("Scores(B/W): $blacks/$whites, Status: $status")

        turn++
    }

    totalMem = Runtime.getRuntime().totalMemory() / 1024
    freeMem = Runtime.getRuntime().freeMemory() / 1024
    println("Memory (totalKB/usedKB): $totalMem/${totalMem - freeMem}")
    println("Board cache size/hit: ${BoardCache.size}/${BoardCache.cacheHit}")
}

fun api() {
    embeddedServer(Netty, port = 8080) {
        install(CORS) {
            anyHost()
        }
        routing {
            findNextMove()
        }
    }.start(wait = true)
}

fun Route.findNextMove() {
    get("/find-next-move") handler@ {
        val player = call.request.queryParameters["player"]
        val board = call.request.queryParameters["board"]
        val passes = call.request.queryParameters["passes"]
        val level = call.request.queryParameters["level"]
        if (player == null || board == null || passes == null || level == null) {
            call.respondText(jsonError("Invalid request"), ContentType.Application.Json, HttpStatusCode.fromValue(400))
            return@handler
        }
        if (player != "BLACK" && player != "WHITE") {
            call.respondText(jsonError("Invalid player"), ContentType.Application.Json, HttpStatusCode.fromValue(400))
            return@handler
        }
        if (!"^[0-9a-f]+(-[0-9a-f]+){3}$".toRegex().matches(board)) {
            call.respondText(jsonError("Invalid board"), ContentType.Application.Json, HttpStatusCode.fromValue(400))
            return@handler
        }
        try {
            if (passes.toInt() !in 0..2) {
                call.respondText(jsonError("Pass count out of range"), ContentType.Application.Json, HttpStatusCode.fromValue(400))
                return@handler
            }
        } catch (ex: NumberFormatException) {
            call.respondText(jsonError("Invalid pass count"), ContentType.Application.Json, HttpStatusCode.fromValue(400))
            return@handler
        }
        try {
            if (level.toInt() !in 1..10) {
                call.respondText(jsonError("Level out of range"), ContentType.Application.Json, HttpStatusCode.fromValue(400))
                return@handler
            }
        } catch (ex: NumberFormatException) {
            call.respondText(jsonError("Invalid level"), ContentType.Application.Json, HttpStatusCode.fromValue(400))
            return@handler
        }
        val nextMove = findNextMove(Board(BoardStorage(board)), Player.valueOf(player), passes.toInt(), level.toInt())
        val response = mapOf(
            "move" to arrayOf(nextMove.x, nextMove.y),
            "passes" to nextMove.passes,
            "board" to nextMove.toHex(),
        )
        println(nextMove)
        call.respondText(jsonResponse(response), ContentType.Application.Json, HttpStatusCode.fromValue(200))
    }
}

fun jsonError(msg: String) = "{\"error\": \"$msg\"}"

fun jsonResponse(response: Map<String, Any>) = Gson().toJson(response)!!

fun findNextMove(board: Board, player: Player, passes: Int, level: Int): Board {
    for (i in 0 until passes) {
        board.pass()
    }
    val mcts = MonteCarloTreeSearch(level)
    var nextMove = mcts.findNextMove(board, player)
    if (nextMove == null) {
        nextMove = board
        nextMove.pass()
    }
    return nextMove
}

fun main(args: Array<String>) {
    if (args.size > 0) {
        when (args[0]) {
            "replay" -> replay(args[1]);
            "simulate" -> simulate();
            else -> println("Usage: java hk.ring0.othello.MainKt [replay <file> | simulate]");
        }
    } else {
        api()
    }
}
