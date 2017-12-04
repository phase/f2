package f2.ir

import f2.type.Type

data class IrModule(
        val name: String,
        val externalFunctions: List<IrExternalFunction>,
        val functions: List<IrFunction>,
        val structs: List<IrStruct>
)

data class IrExternalFunction(
        val name: String,
        val returnType: Type,
        val arguments: List<Type>
)

data class IrFunction(
        val name: String,
        val returnType: Type,
        val argumentCount: Int,
        val registerTypes: List<Type>,
        val instructions: List<Instruction>
)

class IrStruct(
        name: String,
        val fields: List<Type>
) : Type(name)

interface Instruction
interface ValueInstruction : Instruction // Produces a value
interface VoidInstruction : Instruction // Doesn't produce a value

data class StoreInstruction(
        val register: Int
) : VoidInstruction

data class FunctionCallInstruction(
        val functionName: String,
        val registerIndexes: List<Int>
) : ValueInstruction

data class ReturnInstruction(
        val registerIndex: Int
) : VoidInstruction

data class FieldGetInstruction(
        val registerIndex: Int,
        val fieldIndex: Int
) : ValueInstruction

data class FieldSetInstruction(
        val structRegisterIndex: Int,
        val fieldIndex: Int,
        val valueRegisterIndex: Int
) : VoidInstruction

data class HeapAllocateInstruction(
        val type: IrStruct
) : ValueInstruction

data class StackAllocateInstruction(
        val type: IrStruct
) : ValueInstruction
