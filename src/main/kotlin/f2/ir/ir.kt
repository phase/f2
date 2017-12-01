package f2.ir

import f2.ast.Type

data class IrModule(
        val name: String,
        val functions: List<IrFunction>
)

data class IrFunction(
        val name: String,
        val returnType: Type,
        val argumentCount: Int,
        val registerTypes: List<Type>,
        val instructions: List<Instruction>
)

interface Instruction

data class AssignRegisterInstruction(
        val regIndex: Int
) : Instruction

data class FunctionCallInstruction(
        val functionName: String,
        val registerIndexes: List<Int>
) : Instruction

data class ReturnInstruction(
        val registerIndex: Int
) : Instruction
