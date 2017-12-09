package f2.ast

import f2.permission.Permission
import f2.type.Type
import f2.type.UndefinedType

data class AstModule(
        val name: String,
        val functionDeclarations: List<AstFunctionDeclaration>,
        val functionDefinitions: List<AstFunctionDefinition>,
        val structs: List<AstStruct>,
        val traits: List<AstTrait>
) {
    fun getType(name: String, localVariables: Map<String, Type>): Type {
        if (localVariables.containsKey(name)) return localVariables[name]!!

        val functions = functionDeclarations.filter { it.name == name }
        if (functions.isNotEmpty()) return functions[0].returnType

        return UndefinedType
    }

    fun getArgumentType(functionName: String, index: Int): Type {
        val functions = functionDeclarations.filter { it.name == functionName }
        if (functions.isNotEmpty()) return functions[0].argumentTypes[index]

        return UndefinedType
    }

    fun getStruct(structName: String): AstStruct {
        val structs = structs.filter { it.name == structName }
        if (structs.isNotEmpty()) return structs[0]
        // TODO: Real errors
        throw Exception("$structName not found in Module $name")
    }
}

data class AstFunctionDeclaration(
        val name: String,
        val argumentTypes: List<Type>,
        val returnType: Type,
        val permissions: List<Permission>
)

data class AstFunctionDefinition(
        val name: String,
        val arguments: List<String>,
        val statements: List<Statement>
)

data class AstField(
        val name: String,
        val type: Type
)

class AstStruct(
        name: String,
        val fields: List<AstField>,
        val traits: List<AstTrait>,
        val functionDeclarations: List<AstFunctionDeclaration>,
        val functionDefinitions: List<AstFunctionDefinition>
) : Type(name) {
    override fun toString(): String {
        return "AstStruct{$name}"
    }
}

class AstTrait(
        name: String,
        val functionDeclarations: List<AstFunctionDeclaration>
) : Type(name)

