package org.polyfrost.compose.render

public class PolyPath {
    public var evenOdd: Boolean = false

    private var ops = IntArray(16)
    private var args = FloatArray(64)
    private var opCount = 0
    private var argCount = 0

    public fun moveTo(x: Float, y: Float): Unit = op(MOVE, x, y)
    public fun lineTo(x: Float, y: Float): Unit = op(LINE, x, y)

    public fun tangentArcTo(x1: Float, y1: Float, x2: Float, y2: Float, radius: Float): Unit = op(ARC, x1, y1, x2, y2, radius)
    public fun close(): Unit = op(CLOSE)

    public interface Visitor {
        public fun moveTo(x: Float, y: Float)
        public fun lineTo(x: Float, y: Float)
        public fun tangentArcTo(x1: Float, y1: Float, x2: Float, y2: Float, radius: Float)
        public fun close()
    }

    public fun visit(v: Visitor) {
        var a = 0
        for (i in 0 until opCount) {
            when (ops[i]) {
                MOVE -> { v.moveTo(args[a], args[a + 1]); a += 2 }
                LINE -> { v.lineTo(args[a], args[a + 1]); a += 2 }
                ARC -> { v.tangentArcTo(args[a], args[a + 1], args[a + 2], args[a + 3], args[a + 4]); a += 5 }
                CLOSE -> v.close()
            }
        }
    }

    private fun op(op: Int, vararg a: Float) {
        if (opCount == ops.size) ops = ops.copyOf(ops.size * 2)
        if (argCount + a.size > args.size) args = args.copyOf(maxOf(args.size * 2, argCount + a.size))
        ops[opCount++] = op
        a.copyInto(args, argCount)
        argCount += a.size
    }

    private companion object {
        const val MOVE = 0
        const val LINE = 1
        const val ARC = 2
        const val CLOSE = 3
    }
}
