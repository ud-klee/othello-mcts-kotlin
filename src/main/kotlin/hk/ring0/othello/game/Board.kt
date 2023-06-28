package hk.ring0.othello.game

enum class Player(val id: Int) {
    BLACK(1), WHITE(2);

    val opponent inline get() = when (this) {
        BLACK -> WHITE
        WHITE -> BLACK
    }

    fun hasWon(status: Status): Boolean {
        return status.ordinal == id
    }
}

enum class Status {
    ONGOING, BLACK_WIN, WHITE_WIN, DRAW
}

typealias Visitor = (x: Int, y: Int, f: List<IntArray>) -> Unit

data class Move(val x: Int, val y: Int, val flippables: List<IntArray>)

val DX = intArrayOf(-1, 0, 1, -1, 1, -1, 0, 1)
val DY = intArrayOf(-1, -1, -1, 0, 0, 1, 1, 1)

class Board {
    private val store: BoardStorage
    var passes = 0
        private set
    var x = -1
        private set
    var y = -1
        private set

    constructor() : this(BoardStorage()) {
        this[3, 3] = 2
        this[4, 3] = 1
        this[3, 4] = 1
        this[4, 4] = 2
    }

    constructor(store: BoardStorage) {
        this.store = store
    }

    override fun hashCode(): Int {
        return store.hashCode() + 31 * passes + 31 * 31 * x + 31 * 31 * 31 * y
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Board) {
            return false
        }
        return store == other.store && passes == other.passes && x == other.x && y == other.y
    }

    fun toHex() = store.toHex()

    fun copy(): Board {
        val tempBoard = Board(store.copy())
        tempBoard.passes = passes
        tempBoard.x = x
        tempBoard.y = y
        return tempBoard
    }

    operator fun get(x: Int, y: Int): Int {
        return store.get(x, y)
    }

    private operator fun set(x: Int, y: Int, value: Int) {
        store.set(x, y, value)
    }

    override fun toString(): String {
        return if (passes > 0) {
            stringify(-1, -1)
        } else {
            stringify(x, y)
        }
    }

    fun stringify(ax: Int, ay: Int): String {
        return buildString {
            for (y in 0..7) {
                for (x in 0..7) {
                    val n = this@Board[x, y]
                    append(if (x == ax && y == ay) '>' else ' ')
                    append(when (n) {
                        0 -> "."
                        1 -> "x"
                        2 -> "o"
                        else -> "?"
                    })
                }
                append("\n")
            }
        }
    }

    fun getStatus(player: Player): Status {
        val (black, white) = getScores()
        if (black + white < 64) {
            val moves = findPossibleMoves(player)
            if (moves.size > 0 || (moves.size == 0 && passes == 0)) {
                return Status.ONGOING
            }
        }
        if (black > white) {
            return Status.BLACK_WIN
        }
        if (white > black) {
            return Status.WHITE_WIN
        }
        return Status.DRAW
    }

    fun getScores(): IntArray {
        val (_, black, white) = store.count()
        return intArrayOf(black, white)
    }

    fun findPossibleStates(player: Player): List<Board> {
        val states = mutableListOf<Board>()
        val cached = BoardCache.get(this, player)
        if (cached != null && cached.size > 0) {
            states.addAll(cached.map(Board::copy))
        } else {
            explore(player) { x, y, flippables ->
                if (flippables.size > 0) {
                    val tempBoard = this.copy()
                    tempBoard.flip(flippables, player)
                    tempBoard[x, y] = player.id
                    tempBoard.passes = 0
                    tempBoard.x = x
                    tempBoard.y = y
                    states += tempBoard
                }
            }
            if (states.size == 0) {
                val tempBoard = this.copy()
                tempBoard.passes = passes + 1
                tempBoard.x = -1
                tempBoard.y = -1
                states += tempBoard
            }
            BoardCache.add(this, player, states)
        }
        return states
    }

    fun findPossibleMoves(player: Player): List<Move> {
        val moves = mutableListOf<Move>()
        explore(player) { x, y, flippables ->
            if (flippables.size > 0) {
                moves += Move(x, y, flippables)
            }
        }
        return moves
    }

    private fun explore(player: Player, fn: Visitor) {
        for (y in 0..7) {
            for (x in 0..7) {
                if (this[x, y] != 0) {
                    continue
                }
                val flippables = findFlippables(x, y, player)
                fn(x, y, flippables)
            }
        }
    }

    fun play(move: Move, player: Player) {
        if (move.flippables.size == 0) {
            throw IllegalArgumentException("Invalid move")
        }
        flip(move.flippables, player)
        this[move.x, move.y] = player.id
        passes = 0
        x = move.x
        y = move.y
    }

    fun play(x: Int, y: Int, player: Player) {
        val flippables = findFlippables(x, y, player)
        play(Move(x, y, flippables), player)
    }

    fun pass() {
        passes++
        x = -1
        y = -1
    }

    private fun flip(flippables: List<IntArray>, player: Player) {
        for ((fx, fy) in flippables) {
            this[fx, fy] = player.id
        }
    }

    private fun findFlippables(x: Int, y: Int, player: Player): List<IntArray> {
        val flippables = mutableListOf<IntArray>()
        for (i in 0..7) {
            var dx = DX[i] + x
            var dy = DY[i] + y
            val path = mutableListOf<IntArray>()

            while (dx in 0..7 && dy in 0..7) {
                val cell = this[dx, dy]
                if (cell == 0) {
                    break
                }
                if (cell == player.id) {
                    flippables.addAll(path)
                    break
                }
                path += intArrayOf(dx, dy)
                dx += DX[i]
                dy += DY[i]
            }
        }
        return flippables
    }

}
