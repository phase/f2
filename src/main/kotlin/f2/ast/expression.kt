package f2.ast

interface Expression {
    fun debugInfo() : DebugInfo
}

data class IdentifierExpression(
        val debugInfo: DebugInfo,
        val name: String
) : Expression { override fun debugInfo(): DebugInfo = debugInfo }

data class FunctionCallExpression(
        val debugInfo: DebugInfo,
        val functionName: String,
        val arguments: List<Expression>
) : Expression { override fun debugInfo(): DebugInfo = debugInfo }

data class FieldGetterExpression(
        val debugInfo: DebugInfo,
        val structName: String,
        val fieldName: String
) : Expression { override fun debugInfo(): DebugInfo = debugInfo }

data class AllocateStructExpression(
        val debugInfo: DebugInfo,
        val struct: String,
        val expressions: List<Expression>
) : Expression { override fun debugInfo(): DebugInfo = debugInfo }

interface Statement {
    fun debugInfo() : DebugInfo
}

data class VariableAssignmentStatement(
        val debugInfo: DebugInfo,
        val name: String,
        val expression: Expression
) : Statement { override fun debugInfo(): DebugInfo = debugInfo }

data class ReturnStatement(
        val debugInfo: DebugInfo,
        val expression: Expression
) : Statement { override fun debugInfo(): DebugInfo = debugInfo }

data class FieldSetterStatement(
        val debugInfo: DebugInfo,
        val structName: String,
        val fieldName: String,
        val expression: Expression
) : Statement { override fun debugInfo(): DebugInfo = debugInfo }
