package hk.ring0.othello.game

class BoardStorage {
    private val cells: IntArray = intArrayOf(0, 0, 0, 0)

    fun copy(): BoardStorage {
        val copy = BoardStorage()
        copy.cells[0] = cells[0]
        copy.cells[1] = cells[1]
        copy.cells[2] = cells[2]
        copy.cells[3] = cells[3]
        return copy
    }

    fun get(x: Int, y: Int): Int {
        val offset = if ((y and 1) == 1) 0 else 16
        val bits = cells[y shr 1] ushr offset
        return (bits ushr ((7 - x) shl 1)) and 3
    }

    fun set(x: Int, y: Int, value: Int) {
        val offset = (x shl 1) + (if ((y and 1) == 0) 0 else 16)
        val bits = (value shl 30) ushr offset
        val mask = (0xc0000000 shr offset).inv().toInt()
        val i = y shr 1
        cells[i] = cells[i] and mask or bits
    }

    fun count(): IntArray {
        val result = intArrayOf(0, 0, 0)
        for (i in 0..3) {
            var cell = cells[i]
            for (j in 0..15) {
                val k = cell and 3
                result[k]++
                cell = cell ushr 2
            }
        }
        return result
    }
}
