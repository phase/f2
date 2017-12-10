package f2.ir

import f2.ast.DebugInfo
import f2.ast.reportError
import f2.permission.Permission
import f2.type.Type

data class IrModule(
        val name: String,
        val externalFunctions: List<IrExternalFunction>,
        val functions: List<IrFunction>,
        val structs: List<IrStruct>,
        val source: String,
        val errors: MutableList<String>
) {
    fun error(source: String, debugInfo: DebugInfo, message: String) {
        errors.add(reportError(source, debugInfo, message))
    }
}

data class IrExternalFunction(
        val name: String,
        val returnType: Type,
        val arguments: List<Type>,
        val permissions: List<Permission>,
        val debugInfo: DebugInfo
)

data class IrFunction(
        val name: String,
        val returnType: Type,
        val permissions: List<Permission>,
        val argumentCount: Int,
        val registerTypes: List<Type>,
        val instructions: List<Instruction>,
        val debugInfo: DebugInfo
)

class IrStruct(
        name: String,
        val fields: List<Type>,
        val debugInfo: DebugInfo
) : Type(name)

interface Instruction
interface ValueInstruction : Instruction // Produces a value
interface VoidInstruction : Instruction // Doesn't produce a value

data class StoreInstruction(
        val debugInfo: DebugInfo,
        val register: Int
) : VoidInstruction

data class FunctionCallInstruction(
        val debugInfo: DebugInfo,
        val functionName: String,
        val registerIndexes: List<Int>
) : ValueInstruction

data class ReturnInstruction(
        val debugInfo: DebugInfo,
        val registerIndex: Int
) : VoidInstruction

data class FieldGetInstruction(
        val debugInfo: DebugInfo,
        val registerIndex: Int,
        val fieldIndex: Int
) : ValueInstruction

data class FieldSetInstruction(
        val debugInfo: DebugInfo,
        val structRegisterIndex: Int,
        val fieldIndex: Int,
        val valueRegisterIndex: Int
) : VoidInstruction

data class HeapAllocateInstruction(
        val debugInfo: DebugInfo,
        val type: IrStruct
) : ValueInstruction

data class StackAllocateInstruction(
        val debugInfo: DebugInfo,
        val type: IrStruct
) : ValueInstruction
