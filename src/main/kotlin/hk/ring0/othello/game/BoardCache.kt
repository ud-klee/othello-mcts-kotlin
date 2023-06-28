package hk.ring0.othello.game

object BoardCache {
  private val blackCache = mutableMapOf<Board, List<Board>>()
  private val whiteCache = mutableMapOf<Board, List<Board>>()

  val size get() = blackCache.size + whiteCache.size

  var cacheHit = 0
      private set

  fun has(board: Board, player: Player) = when (player) {
      Player.BLACK -> blackCache.containsKey(board)
      Player.WHITE -> whiteCache.containsKey(board)
  }

  fun add(board: Board, player: Player, nextStates: List<Board>) = when (player) {
      Player.BLACK -> blackCache[board] = nextStates
      Player.WHITE -> whiteCache[board] = nextStates
  }

  fun get(board: Board, player: Player): List<Board>? {
      val value = when (player) {
          Player.BLACK -> blackCache[board]
          Player.WHITE -> whiteCache[board]
      }
      if (value != null) {
          cacheHit++
      }
      return value
  }
}
