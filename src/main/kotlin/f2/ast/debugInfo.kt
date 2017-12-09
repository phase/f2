package f2.ast

import org.antlr.v4.runtime.ParserRuleContext

data class DebugInfo(val line: Int, val column: Int)

data class Error(val debugInfo: DebugInfo, val message: String)

fun ParserRuleContext.debugInfo(): DebugInfo {
    val line = this.start.line
    val column = this.start.charPositionInLine
    return DebugInfo(line, column)
}


fun reportError(source: String, debugInfo: DebugInfo, message: String) {
    val line = debugInfo.line - 1
    val lines = source.split("\n")

    val before = if (line > 0) "$line| ${lines[line - 1]}\n" else ""
    val errorLine = "${line + 1}| ${lines[line]}\n"
    val after = if (line + 1 < lines.size) "${line + 2}| ${lines[line + 1]}" else ""

    val column = debugInfo.column
    val arrow = " ".repeat(line.toString().length) + "| " + "~".repeat(column) + "^"

    val error = "$before$errorLine$arrow\n$after\nError: $message\n"

    // TODO Maybe not exit immediately?
    System.err.print(error)
    System.exit(-1)
}
