package f2.ast

interface Expression

data class IdentifierExpression(
        val name: String
) : Expression

data class FunctionCallExpression(
        val functionName: String,
        val arguments: List<Expression>
) : Expression

interface Statement

data class VariableAssignmentStatement(
        val name: String,
        val expression: Expression
) : Statement
