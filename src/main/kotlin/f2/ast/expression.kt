package f2.ast

import f2.type.Type

interface Expression {
    val debugInfo: DebugInfo
}

data class IdentifierExpression(
        override val debugInfo: DebugInfo,
        val name: String
) : Expression

data class TypeExpression(
        override val debugInfo: DebugInfo,
        val type: Type
) : Expression

data class FunctionCallExpression(
        override val debugInfo: DebugInfo,
        val functionName: String,
        val typeParameters: List<TypeExpression> = listOf(),
        val arguments: List<Expression>
) : Expression

data class FieldGetterExpression(
        override val debugInfo: DebugInfo,
        val structName: String,
        val fieldName: String
) : Expression

data class AllocateStructExpression(
        override val debugInfo: DebugInfo,
        val struct: String,
        val expressions: List<Expression>
) : Expression

/* ========================================= */

interface Statement {
    val debugInfo: DebugInfo
}

data class VariableAssignmentStatement(
        override val debugInfo: DebugInfo,
        val name: String,
        val expression: Expression
) : Statement

data class ReturnStatement(
        override val debugInfo: DebugInfo,
        val expression: Expression
) : Statement

data class FieldSetterStatement(
        override val debugInfo: DebugInfo,
        val structName: String,
        val fieldName: String,
        val expression: Expression
) : Statement
