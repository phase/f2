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
        convert(astModule, dec, it, irStructs)
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
        astFunctionDefinition: AstFunctionDefinition,
        irStructs: List<IrStruct>
): IrFunction {
    val functionName = astFunctionDeclaration.name
    val returnType = astFunctionDeclaration.returnType
    val argCount = astFunctionDeclaration.argumentTypes.size
    val variables: MutableMap<String, Type> = astFunctionDefinition.arguments.mapIndexed { i, s -> s to astFunctionDeclaration.argumentTypes[i] }.toMap().toMutableMap()
    val registerIndexes: MutableMap<String, Int> = astFunctionDefinition.arguments.mapIndexed { i, s -> s to i }.toMap().toMutableMap()
    val registers: MutableList<Type> = astFunctionDeclaration.argumentTypes.toMutableList()
    val instructions: MutableList<Instruction> = mutableListOf()

    // return type of expression
    fun typeCheck(exp: Expression): Type {
        return when (exp) {
            is IdentifierExpression -> {
                astModule.getType(exp.name, variables)
            }
            is FunctionCallExpression -> {
                exp.arguments.forEachIndexed { i, e ->
                    val argType = typeCheck(e)
                    if (e !is IdentifierExpression) {
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
                val struct = variables[exp.structName]!! as AstStruct
                struct.fields.filter { it.name == exp.fieldName }.last().type
            }
            is AllocateStructExpression -> {
                exp.expressions.forEach { typeCheck(it) }
                astModule.getStruct(exp.struct)
            }
            else -> {
                println(exp)
                UndefinedType
            }
        }
    }

    val statements = astFunctionDefinition.statements

    // type check statements and insert variables into the register tables
    statements.forEach {
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

    var registerIndex = 0

    // returns the register index the expression goes into
    fun generateExpression(exp: Expression): Int {
        return when (exp) {
            is FunctionCallExpression -> {
                instructions.add(FunctionCallInstruction(
                        exp.functionName,
                        exp.arguments.map {
                            val i = generateExpression(it)
                            // if the expression is an identifier, that means there is already a register for it and
                            // we don't need to store it in another one
                            if (it !is IdentifierExpression) {
                                instructions.add(StoreInstruction(i))
                            }
                            i
                        }
                ))
                registerIndex++ + argCount
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
                registerIndex++ + argCount
            }
            is AllocateStructExpression -> {
                val structs = irStructs.filter { it.name == exp.struct }
                if (structs.isNotEmpty()) {
                    val irStruct = structs.last()
                    instructions.add(HeapAllocateInstruction(irStruct))
                    val structIndex = registerIndex++ + argCount
                    instructions.add(StoreInstruction(structIndex))
                    // keep track of identified registers
                    var idRegs = 0
                    // generate the field expressions and set them in the allocation
                    val setFieldInstructions = exp.expressions.mapIndexed { i, e ->
                        if (e is IdentifierExpression) idRegs++
                        FieldSetInstruction(structIndex, i, generateExpression(e))
                    }
                    instructions.addAll(setFieldInstructions)
                    registerIndex += setFieldInstructions.size - idRegs
                    structIndex
                } else throw Exception("Can't find ${exp.struct}")
            }
            else -> -1
        }
    }

    // go through them again and generate the instructions
    statements.forEach {
        when (it) {
            is VariableAssignmentStatement -> {
                val i = generateExpression(it.expression)
                if (it.expression !is AllocateStructExpression)
                    instructions.add(StoreInstruction(i))
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
