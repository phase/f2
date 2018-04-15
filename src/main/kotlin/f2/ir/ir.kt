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
    val debugInfo: DebugInfo
}

interface ValueInstruction : Instruction // Produces a value
interface VoidInstruction : Instruction // Doesn't produce a value

data class StoreInstruction(
        override val debugInfo: DebugInfo,
        val register: Int
) : VoidInstruction

data class FunctionCallInstruction(
        override val debugInfo: DebugInfo,
        val functionName: String,
        val registerIndexes: List<Int>
) : ValueInstruction

data class ReturnInstruction(
        override val debugInfo: DebugInfo,
        val registerIndex: Int
) : VoidInstruction

data class FieldGetInstruction(
        override val debugInfo: DebugInfo,
        val registerIndex: Int,
        val fieldIndex: Int
) : ValueInstruction

data class FieldSetInstruction(
        override val debugInfo: DebugInfo,
        val structRegisterIndex: Int,
        val fieldIndex: Int,
        val valueRegisterIndex: Int
) : VoidInstruction

data class HeapAllocateInstruction(
        override val debugInfo: DebugInfo,
        val type: IrStruct
) : ValueInstruction

data class StackAllocateInstruction(
        override val debugInfo: DebugInfo,
        val type: IrStruct
) : ValueInstruction

data class FreeAllocationInstruction(
        override val debugInfo: DebugInfo,
        val register: Int
) : VoidInstruction
