package hk.ring0.othello

import hk.ring0.othello.game.Board
import hk.ring0.othello.game.BoardStorage
import hk.ring0.othello.game.Player
import hk.ring0.othello.game.Move
import hk.ring0.othello.game.BoardCache
import hk.ring0.othello.mcts.Node
import hk.ring0.othello.mcts.State
import hk.ring0.othello.mcts.Tree
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OthelloTest {

    @Test
    fun givenNewBoard_whenInitialized_thenBoardStringifiedCorrectly() {
        val board = Board()

        assertThat(board.toString().trim()).isEqualTo("""
            | . . . . . . . .
            | . . . . . . . .
            | . . . . . . . .
            | . . . o x . . .
            | . . . x o . . .
            | . . . . . . . .
            | . . . . . . . .
            | . . . . . . . .
            |""".trimMargin().trim())
    }

    @Test
    fun givenEmptyStorage_whenCellsRepeatedlySet_thenCellsSetCorrectly() {
        val store = BoardStorage()

        for (y in 0..7) {
            for (x in 0..7) {
                store.set(x, y, 1)
                store.set(x, y, 0)
                store.set(x, y, 2)
            }
        }

        for (y in 0..7) {
            for (x in 0..7) {
                assertThat(listOf(x, y, store.get(x, y))).isEqualTo(listOf(x, y, 2))
            }
        }
    }

    @Test
    fun givenEmptyStorage_whenInitialized_thenCountCorrectly() {
        val store = BoardStorage()
        for (i in 0..7) {
            store.set(i, i, (i and 1) + 1)
        }
        val count = store.count()
        assertThat(count.toList()).isEqualTo(listOf(56, 4, 4))
    }

    @Test
    fun givenNewBoard_whenFindMove_thenFoundValidMoves() {
        val board = Board()
        val moves = board.findPossibleMoves(Player.BLACK)

        assertThat(moves.map({ (x, y) -> listOf(x, y) }))
            .isEqualTo(listOf(
                listOf(3, 2),
                listOf(2, 3),
                listOf(5, 4),
                listOf(4, 5),
            ))
    }

    @Test
    fun givenNewBoard_whenPlay_thenFlipCorrectly() {
        val board0 = Board()
        val moves = board0.findPossibleMoves(Player.BLACK)

        val board1 = board0.copy()
        board1.play(moves[0], Player.BLACK)
        assertThat(board1.toString().trim()).isEqualTo("""
            | . . . . . . . .
            | . . . . . . . .
            | . . . x . . . .
            | . . . x x . . .
            | . . . x o . . .
            | . . . . . . . .
            | . . . . . . . .
            | . . . . . . . .
            |""".trimMargin().trim())
    }

    @Test
    fun givenSituation_whenFindMove_thenFoundNoMoves() {
        val store = BoardStorage()
        val board = Board(store)
        store.set(0, 0, 2)

        val moves = board.findPossibleMoves(Player.BLACK)
        assertThat(moves).isEqualTo(emptyList<Move>())
    }

    @Test
    fun givenEmptyTree_whenInitialized_thenStructurallyCorrect() {
        val state = State(Board())
        val root = Node(state)
        val tree = Tree(root)
        val nodeA = Node(state)
        val nodeB = Node(state)
        val nodeC = Node(state)
        val nodeD = Node(state)
        val nodeE = Node(state)

        root += nodeA
        root += nodeB
        nodeA += nodeC
        nodeA += nodeD
        nodeB += nodeE

        assertThat(nodeA.parent).isSameAs(root)
        assertThat(nodeB.parent).isSameAs(root)
        assertThat(nodeC.parent).isSameAs(nodeA)
        assertThat(nodeD.parent).isSameAs(nodeA)
        assertThat(nodeE.parent).isSameAs(nodeB)
        val (size, height) = tree.dimension
        assertThat(size).isEqualTo(6)
        assertThat(height).isEqualTo(3)
    }

    @Test
    fun givenSomeBoards_whenCached_thenCacheCorrectly() {
        val boards = List<Board>(4) { Board() }
        boards[2].pass()
        boards[3].pass()

        val nothing = emptyList<Board>()

        BoardCache.add(boards[0], Player.BLACK, nothing)
        BoardCache.add(boards[1], Player.BLACK, nothing)
        BoardCache.add(boards[2], Player.BLACK, nothing)
        BoardCache.add(boards[3], Player.BLACK, nothing)
        BoardCache.add(boards[0], Player.WHITE, nothing)
        BoardCache.add(boards[1], Player.WHITE, nothing)
        BoardCache.add(boards[2], Player.WHITE, nothing)
        BoardCache.add(boards[3], Player.WHITE, nothing)

        BoardCache.get(boards[0], Player.BLACK)
        BoardCache.get(boards[1], Player.BLACK)
        BoardCache.get(boards[2], Player.BLACK)
        BoardCache.get(boards[3], Player.BLACK)
        BoardCache.get(boards[0], Player.WHITE)
        BoardCache.get(boards[1], Player.WHITE)
        BoardCache.get(boards[2], Player.WHITE)
        BoardCache.get(boards[3], Player.WHITE)

        assertThat(BoardCache.size).isEqualTo(4)
        assertThat(BoardCache.cacheHit).isEqualTo(8)
    }
}
