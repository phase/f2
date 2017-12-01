package f2.ir

import f2.ast.*

fun convert(astModule: AstModule): IrModule {
    val irFunctions = astModule.functionDefinitions.map {
        val defName = it.name
        val dec = run {
            val possibleDecs = astModule.functionDeclarations.filter { it.name == defName }
            if (possibleDecs.isNotEmpty()) possibleDecs[0] else {
                AstFunctionDeclaration(defName, (0..it.arguments.size - 1).map { UndefinedType }, UndefinedType)
            }
        }
        convert(astModule, dec, it)
    }
    return IrModule(astModule.name, irFunctions)
}

fun convert(
        astModule: AstModule,
        astFunctionDeclaration: AstFunctionDeclaration,
        astFunctionDefinition: AstFunctionDefinition
): IrFunction {
    val functionName = astFunctionDeclaration.name
    val returnType = astFunctionDeclaration.returnType
    val variables: MutableMap<String, Type> = astFunctionDefinition.arguments.mapIndexed { i, s -> s to astFunctionDeclaration.argumentTypes[i] }.toMap().toMutableMap()
    val registerIndexes: MutableMap<String, Int> = astFunctionDefinition.arguments.mapIndexed { i, s -> s to i }.toMap().toMutableMap()
    val registers: MutableList<Type> = astFunctionDeclaration.argumentTypes.toMutableList()
    val instructions: MutableList<Instruction> = mutableListOf()

    fun typeCheck(exp: Expression): Type {
        return when (exp) {
            is IdentifierExpression -> {
                astModule.getType(exp.name, variables)
            }
            is FunctionCallExpression -> {
                exp.arguments.forEachIndexed { i, e ->
                    val argType = typeCheck(e)
                    if (e is IdentifierExpression && argType == UndefinedType) {
                        val argInFunType = astModule.getArgumentType(exp.functionName, i)
                        val varName = e.name
                        variables.put(varName, argInFunType)
                        val regIndex = registerIndexes[varName]!!
                        registers[regIndex] = argInFunType
                    }
                }
                astModule.getType(exp.functionName, mapOf())
            }
            else -> UndefinedType
        }
    }

    val statements = astFunctionDefinition.statements

    // type check statements and insert variables into the register tables
    statements.map {
        when (it) {
            is VariableAssignmentStatement -> {
                val type = typeCheck(it.expression)
                variables.put(it.name, type)
                registerIndexes.put(it.name, registers.size)
                registers.add(type)
            }
            is ReturnStatement -> {
                typeCheck(it.expression)
            }
            else -> {
            }
        }
    }

    // returns the register index the expression goes into
    fun generateExpression(exp: Expression, reg: Int): Int {
        return when (exp) {
            is FunctionCallExpression -> {
                instructions.add(FunctionCallInstruction(
                        exp.functionName,
                        exp.arguments.map { generateExpression(it, reg) }
                ))
                reg
            }
            is IdentifierExpression -> registerIndexes[exp.name]!!
            else -> -1
        }
    }

    // go through them again and generate the instructions
    statements.map {
        when (it) {
            is VariableAssignmentStatement -> {
                val i = registerIndexes[it.name]!!
                generateExpression(it.expression, i)
                instructions.add(AssignRegisterInstruction(i))
            }
            is ReturnStatement -> {
                val i = generateExpression(it.expression, -1)
                instructions.add(ReturnInstruction(i))
            }
            else -> {
            }
        }
    }

    return IrFunction(functionName, returnType, astFunctionDeclaration.argumentTypes.size, registers, instructions)
}
