package f2.ast

interface Expression

data class IdentifierExpression(
        val name: String
) : Expression

data class FunctionCallExpression(
        val functionName: String,
        val arguments: List<Expression>
) : Expression

data class FieldGetterExpression(
        val structName: String,
        val fieldName: String
) : Expression

interface Statement

data class VariableAssignmentStatement(
        val name: String,
        val expression: Expression
) : Statement

data class ReturnStatement(
        val expression: Expression
) : Statement

data class FieldSetterStatement(
        val structName: String,
        val fieldName: String,
        val expression: Expression
) : Statement
