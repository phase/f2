package f2.ast

data class Module(
        val functionDeclarations: List<FunctionDeclaration>,
        val functionDefinitions: List<FunctionDefinition>,
        val structs: List<Struct>,
        val traits: List<Trait>
)

data class FunctionDeclaration(
        val name: String,
        val argumentTypes: List<Type>,
        val returnType: Type
)

data class FunctionDefinition(
        val name: String,
        val arguments: List<String>,
        val statements: List<Statement>,
        val returnExpression: Expression
)

data class Field(
        val name: String,
        val type: Type
)

class Struct(
        name: String,
        val fields: List<Field>,
        val traits: List<Trait>,
        val functionDeclarations: List<FunctionDeclaration>,
        val functionDefinitions: List<FunctionDefinition>
) : Type(name)

class Trait(
        name: String,
        val functionDeclarations: List<FunctionDeclaration>
) : Type(name)

