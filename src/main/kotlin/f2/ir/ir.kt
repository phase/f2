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
    fun error(debugInfo: DebugInfo, message: String) {
        errors.add(reportError(source, debugInfo, message))
    }

    fun getFunction(name: String): IrExternalFunction? {
        val functionsWithName = externalFunctions.filter { it.name == name }.toMutableList()
        functionsWithName.addAll(functions.filter { it.name == name })

        return functionsWithName.lastOrNull()
    }
}

open class IrExternalFunction(
        val name: String,
        val returnType: Type,
        val arguments: List<Type>,
        val permissions: List<Permission>,
        val debugInfo: DebugInfo
) {
    override fun toString(): String {
        return "fun $name(${arguments.mapIndexed { i, t -> "%$i : $t" }.joinToString(",")})" +
                " : $returnType (${permissions.joinToString(" ")})"
    }
}

class IrFunction(
        name: String,
        returnType: Type,
        permissions: List<Permission>,
        val argumentCount: Int,
        val registerTypes: List<Type>,
        val instructions: List<Instruction>,
        debugInfo: DebugInfo
) : IrExternalFunction(name, returnType, registerTypes.subList(0, argumentCount), permissions, debugInfo) {
    override fun toString(): String {
        return "fun $name(${arguments.mapIndexed { i, t -> "%$i : $t" }.joinToString(",")})" +
                " : $returnType (${permissions.joinToString(" ")})\n" +
                instructions.map { "    $it" }.joinToString("\n") + "\n\n"
    }
}

class IrStruct(
        name: String,
        val fields: List<Type>,
        val debugInfo: DebugInfo
) : Type(name)

interface Instruction {
    fun debugInfo(): DebugInfo
}

interface ValueInstruction : Instruction // Produces a value
interface VoidInstruction : Instruction // Doesn't produce a value

data class StoreInstruction(
        val debugInfo: DebugInfo,
        val register: Int
) : VoidInstruction { override fun debugInfo(): DebugInfo = debugInfo }

data class FunctionCallInstruction(
        val debugInfo: DebugInfo,
        val functionName: String,
        val registerIndexes: List<Int>
) : ValueInstruction { override fun debugInfo(): DebugInfo = debugInfo }

data class ReturnInstruction(
        val debugInfo: DebugInfo,
        val registerIndex: Int
) : VoidInstruction { override fun debugInfo(): DebugInfo = debugInfo }

data class FieldGetInstruction(
        val debugInfo: DebugInfo,
        val registerIndex: Int,
        val fieldIndex: Int
) : ValueInstruction { override fun debugInfo(): DebugInfo = debugInfo }

data class FieldSetInstruction(
        val debugInfo: DebugInfo,
        val structRegisterIndex: Int,
        val fieldIndex: Int,
        val valueRegisterIndex: Int
) : VoidInstruction { override fun debugInfo(): DebugInfo = debugInfo }

data class HeapAllocateInstruction(
        val debugInfo: DebugInfo,
        val type: IrStruct
) : ValueInstruction { override fun debugInfo(): DebugInfo = debugInfo }

data class StackAllocateInstruction(
        val debugInfo: DebugInfo,
        val type: IrStruct
) : ValueInstruction { override fun debugInfo(): DebugInfo = debugInfo }

data class FreeAllocationInstruction(
        val debugInfo: DebugInfo,
        val register: Int
) : VoidInstruction { override fun debugInfo(): DebugInfo = debugInfo }
