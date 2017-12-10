package f2.ast

import org.antlr.v4.runtime.ParserRuleContext

data class DebugInfo(val line: Int, val column: Int) {
    override fun toString(): String = "($line,$column)"
}

fun ParserRuleContext.debugInfo(): DebugInfo {
    val line = this.start.line
    val column = this.start.charPositionInLine
    return DebugInfo(line, column)
}

fun reportError(source: String, debugInfo: DebugInfo, message: String): String {
    val line = debugInfo.line - 1
    val lines = source.split("\n")
    val numberLength = (line + 2).toString().length

    // Don't ask.
    fun gen(i: Int): String = if (i > 0 && i - 1 < lines.size) (
            (if (i.toString().length < numberLength) " " else "") +
                    "$i|" +
                    if (lines[i - 1].isNotEmpty()) " ${lines[i - 1]}"
                    else ""
            ) else ""

    val before = gen(line) + "\n"
    val errorLine = gen(line + 1) + "\n"
    val after = gen(line + 2)

    val column = debugInfo.column
    val arrow = " ".repeat(numberLength) + "| " + "~".repeat(column) + "^"

    return "$before$errorLine$arrow\n$after\nError: $message"
}
