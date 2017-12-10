package f2.ir

import f2.ast.*
import f2.permission.ExternalPermission
import f2.type.Type
import f2.type.UndefinedType
import f2.type.primitives

fun convert(astModule: AstModule): IrModule {
    val irStructs = astModule.structs.map {
        convert(astModule, it)
    }

    val irFunctions = astModule.functionDefinitions.map {
        val defName = it.name
        val dec = run {
            val possibleDecs = astModule.functionDeclarations.filter { it.name == defName }
            if (possibleDecs.isNotEmpty()) possibleDecs[0] else {
                // TODO Error
                AstFunctionDeclaration(defName, (0 until it.arguments.size).map { UndefinedType }, UndefinedType, listOf(), DebugInfo(-1, -1))
            }
        }
        convert(astModule, dec, it, irStructs)
    }

    val declarationsWithoutDefinition = astModule.functionDeclarations.filter {
        val decName = it.name
        astModule.functionDefinitions.filter { it.name == decName }.isEmpty()
    }

    val irExternalFunctions = declarationsWithoutDefinition.map { convert(it, irStructs) }
    irExternalFunctions.forEach {
        if (!it.permissions.contains(ExternalPermission)) {
            // TODO Error
        }
    }

    return IrModule(astModule.name, irExternalFunctions, irFunctions, irStructs, astModule.source, astModule.errors)
}

fun convert(
        astModule: AstModule,
        astStruct: AstStruct
): IrStruct {
    return IrStruct(astStruct.name, astStruct.fields.map {
        if (it.type is AstStruct) {
            convert(astModule, it.type)
        } else it.type
    }, astStruct.debugInfo)
}

fun convert(
        astFunctionDeclaration: AstFunctionDeclaration,
        irStructs: List<IrStruct>
): IrExternalFunction {
    val types: MutableList<Type> = irStructs.map { it as Type }.toMutableList()
    types.addAll(primitives)
    val irReturnType = types.find { it.name == astFunctionDeclaration.returnType.name }!!
    val argumentTypes = astFunctionDeclaration.argumentTypes.map {
        val argType = it.name
        types.find { it.name == argType }!!
    }
    return IrExternalFunction(astFunctionDeclaration.name, irReturnType, argumentTypes,
            astFunctionDeclaration.permissions, astFunctionDeclaration.debugInfo)
}

fun convert(
        astModule: AstModule,
        astFunctionDeclaration: AstFunctionDeclaration,
        astFunctionDefinition: AstFunctionDefinition,
        irStructs: List<IrStruct>
): IrFunction {

    fun AstStruct.toIrStruct(): IrStruct = irStructs.find { it.name == this.name }!!

    fun Type.toIrType(): Type = if (this is AstStruct) this.toIrStruct() else this

    val functionName = astFunctionDeclaration.name
    val returnType: Type = astFunctionDeclaration.returnType.toIrType()
    val argCount = astFunctionDeclaration.argumentTypes.size
    val variables: MutableMap<String, Type> = astFunctionDefinition.arguments.mapIndexed { i, s ->
        val astType = astFunctionDeclaration.argumentTypes[i]
        val type = astType.toIrType()
        s to type
    }.toMap().toMutableMap()
    val registerIndexes: MutableMap<String, Int> = astFunctionDefinition.arguments.mapIndexed { i, s -> s to i }.toMap().toMutableMap()
    val registers: MutableList<Type> = astFunctionDeclaration.argumentTypes.map {
        it.toIrType()
    }.toMutableList()
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
                val irStruct = variables[exp.structName]!! as IrStruct
                val astStruct = astModule.getStruct(irStruct.name)
                astStruct.fields.filter { it.name == exp.fieldName }.last().type.toIrType()
            }
            is AllocateStructExpression -> {
                exp.expressions.forEachIndexed { _, e ->
                    val argType = typeCheck(e)
                    if (e !is IdentifierExpression) {
                        registers.add(argType)
                    }
                }
                irStructs.find { it.name == exp.struct }!!
            }
            else -> {
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
                val fieldType = structType.fields.filter { it.name == fieldName }.last().type.toIrType()
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
                        exp.debugInfo,
                        exp.functionName,
                        exp.arguments.map {
                            val i = generateExpression(it)
                            // if the expression is an identifier, that means there is already a register for it and
                            // we don't need to store it in another one
                            if (it !is IdentifierExpression) {
                                instructions.add(StoreInstruction(it.debugInfo(), i))
                            }
                            i
                        }
                ))
                registerIndex++ + argCount
            }
            is IdentifierExpression -> registerIndexes[exp.name]!!
            is FieldGetterExpression -> {
                val structReg = registerIndexes[exp.structName]!!
                val irStruct = variables[exp.structName]!! as IrStruct
                val astStruct = astModule.getStruct(irStruct.name)
                val fieldIndex = astStruct.fields.map { it.name }.indexOf(exp.fieldName)
                instructions.add(FieldGetInstruction(
                        exp.debugInfo,
                        structReg,
                        fieldIndex
                ))
                registerIndex++ + argCount
            }
            is AllocateStructExpression -> {
                val structs = irStructs.filter { it.name == exp.struct }
                if (structs.isNotEmpty()) {
                    // generate the arguments before the struct
                    val expressionRegisters = exp.expressions.map { generateExpression(it) }

                    // generate the struct
                    val irStruct = structs.last()
                    instructions.add(HeapAllocateInstruction(exp.debugInfo, irStruct))
                    val structIndex = registerIndex++ + argCount
                    instructions.add(StoreInstruction(exp.debugInfo, structIndex))

                    // keep track of identified registers
                    var idRegs = 0
                    // generate the field expressions and set them in the allocation
                    val setFieldInstructions = exp.expressions.mapIndexed { i, e ->
                        if (e is IdentifierExpression) idRegs++
                        FieldSetInstruction(e.debugInfo(), structIndex, i, expressionRegisters[i])
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
                    instructions.add(StoreInstruction(it.debugInfo, i))
            }
            is ReturnStatement -> {
                val i = generateExpression(it.expression)
                instructions.add(ReturnInstruction(it.debugInfo, i))
            }
            is FieldSetterStatement -> {
                val structReg = registerIndexes[it.structName]!!
                val struct = variables[it.structName]!! as AstStruct
                val fieldIndex = struct.fields.map { it.name }.indexOf(it.fieldName)
                val i = generateExpression(it.expression)
                instructions.add(FieldSetInstruction(it.debugInfo, structReg, fieldIndex, i))
            }
            else -> {
            }
        }
    }

    return IrFunction(functionName, returnType, astFunctionDeclaration.permissions,
            astFunctionDeclaration.argumentTypes.size, registers, instructions, astFunctionDefinition.debugInfo)
}
