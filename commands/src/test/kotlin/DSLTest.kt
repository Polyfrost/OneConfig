import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.polyfrost.oneconfig.api.commands.factories.dsl.CommandDSL.Companion.command
import org.polyfrost.oneconfig.api.commands.factories.dsl.CommandDSL.Companion.meta

class DSLTest {
    @Test
    fun main() {
        val tree = command("test") {
            command(
                "sup", "hello", metadata = listOf(
                    meta(0, "a", "an integer"),
                    meta(3, "some float", "a float"),
                )
            ) { a: Int, b: String, c: Float, d: Double, e: Byte, f: Float ->
                println(a)
                println(b)
                println(c + f)
                println(e)
                println(d)
            }
            subcmd("jeff") {
                cmd("chicken") {
                    println("chicken")
                }
                cmd("chicken") { a: Int, b: Int ->
                    return@cmd a + b
                }
                subcmd("bob") {
                    cmd("jeff") { c: Double, d: Double ->
                        println(c + d)
                        return@cmd c + d
                    }
                }
            }
        }.registerTree()

        tree.execute("sup", "1", "hello", "3.5", "4.2", "1", "4.5")
        tree.execute("jeff", "chicken")
        assertEquals(3, tree.execute("jeff", "chicken", "1", "2"))
        assertEquals(3.0, tree.execute("jeff", "bob", "jeff", "1.5", "1.5"))
    }
}