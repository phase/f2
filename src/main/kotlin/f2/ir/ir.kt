package f2.ir

import f2.ast.Type

data class IrModule(
        val name: String,
        val functions: List<IrFunction>,
        val structs: List<IrStruct>
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

data class StoreInstruction(
        val register: Int
) : Instruction

data class FunctionCallInstruction(
        val functionName: String,
        val registerIndexes: List<Int>
) : Instruction

data class ReturnInstruction(
        val registerIndex: Int
) : Instruction

data class FieldGetInstruction(
        val registerIndex: Int,
        val fieldIndex: Int
) : Instruction

data class FieldSetInstruction(
        val structRegisterIndex: Int,
        val fieldIndex: Int,
        val valueRegisterIndex: Int
) : Instruction

data class HeapAllocateInstruction(
        val type: IrStruct
) : Instruction

data class StackAllocateInstruction(
        val type: IrStruct
) : Instruction
