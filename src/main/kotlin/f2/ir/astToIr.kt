package f2.ir

import f2.ast.*

fun convert(astModule: AstModule): IrModule {
    val irStructs = astModule.structs.map {
        convert(astModule, it)
    }

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
    return IrModule(astModule.name, irFunctions, irStructs)
}

fun convert(
        astModule: AstModule,
        astStruct: AstStruct
): IrStruct {
    return IrStruct(astStruct.name, astStruct.fields.map { it.type })
}

fun convert(
        astModule: AstModule,
        astFunctionDeclaration: AstFunctionDeclaration,
        astFunctionDefinition: AstFunctionDefinition
): IrFunction {
    val functionName = astFunctionDeclaration.name
    val returnType = astFunctionDeclaration.returnType
    val argCount = astFunctionDeclaration.argumentTypes.size
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
                    if (e !is IdentifierExpression) {
                        println("${registers.size} := $e : $argType")
                        registers.add(argType)
                    }
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
            is FieldGetterExpression -> {
                val struct = astModule.getStruct(exp.structName)
                struct.fields.filter { it.name == exp.fieldName }.last().type
            }
            else -> {
                println(exp)
                UndefinedType
            }
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
            is FieldSetterStatement -> {
                val structType = variables[it.structName]!! as AstStruct
                val fieldName = it.fieldName
                val fieldType = structType.fields.filter { it.name == fieldName }.last().type
                val expType = typeCheck(it.expression)
                assert(expType == fieldType)
            }
            is ReturnStatement -> {
                typeCheck(it.expression)
            }
            else -> {
            }
        }
    }

    // returns the register index the expression goes into
    fun generateExpression(exp: Expression): Int {
        println(exp)
        return when (exp) {
            is FunctionCallExpression -> {
                instructions.add(FunctionCallInstruction(
                        exp.functionName,
                        exp.arguments.map { generateExpression(it) }
                ))
                instructions.size + argCount
            }
            is IdentifierExpression -> registerIndexes[exp.name]!!
            is FieldGetterExpression -> {
                val structReg = registerIndexes[exp.structName]!!
                val struct = variables[exp.structName]!! as AstStruct
                val fieldIndex = struct.fields.map { it.name }.indexOf(exp.fieldName)
                instructions.add(FieldGetInstruction(
                        structReg,
                        fieldIndex
                ))
                instructions.size + argCount
            }
            else -> -1
        }
    }

    // go through them again and generate the instructions
    statements.map {
        println(it)
        when (it) {
            is VariableAssignmentStatement -> {
                generateExpression(it.expression)
            }
            is ReturnStatement -> {
                val i = generateExpression(it.expression)
                instructions.add(ReturnInstruction(i))
            }
            is FieldSetterStatement -> {
                val structReg = registerIndexes[it.structName]!!
                val struct = variables[it.structName]!! as AstStruct
                val fieldIndex = struct.fields.map { it.name }.indexOf(it.fieldName)
                val i = generateExpression(it.expression)
                instructions.add(FieldSetInstruction(structReg, fieldIndex, i))
            }
            else -> {
            }
        }
    }

    return IrFunction(functionName, returnType, astFunctionDeclaration.argumentTypes.size, registers, instructions)
}
