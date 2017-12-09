package f2.ast

import org.antlr.v4.runtime.ParserRuleContext

data class DebugInfo(val line: Int, val column: Int)

fun ParserRuleContext.debugInfo(): DebugInfo {
    val line = this.start.line
    val column = this.start.charPositionInLine
    return DebugInfo(line, column)
}
