package f2.ir

import f2.ast.DebugInfo
import f2.ast.ImportPath
import f2.ast.reportError
import f2.permission.Permission
import f2.type.Type
import java.util.*

open class IrModuleHeader(
        val name: List<String>,
        val structs: List<IrStruct>,
        val functions: MutableList<IrFunctionHeader>,
        val imports: List<ImportPath>
) {

    override fun toString(): String {
        // Don't ask.
        return "module ${name.joinToString(".")}\n${imports.joinToString("\n") { it.joinToString(".") }}\n" +
                "${structs.joinToString("\n") { it.toString() }}\n\n${functions.joinToString("\n\n") { it.toString() }}\n"
    }
}

class IrModule(
        name: List<String>,
        structs: List<IrStruct>,
        functions: MutableList<IrFunctionHeader>,
        imports: List<ImportPath>,
        val source: String,
        val errors: MutableList<String>
) : IrModuleHeader(name, structs, functions, imports) {
    fun error(debugInfo: DebugInfo, message: String) {
        errors.add(reportError(source, debugInfo, message))
    }

    fun getFunction(name: String, knownModules: List<IrModuleHeader>): IrFunctionHeader? {
        val function = functions.find { it.name == name }
        if (function != null) {
            return function
        } else {
            knownModules.forEach {
                val externalFunction = it.functions.find { it.name == name }
                if (externalFunction != null) return externalFunction
            }
        }
        return null
    }
}

open class IrFunctionHeader(
        val name: String,
        val returnType: Type,
        val arguments: List<Type>,
        val permissions: List<Permission>,
        val debugInfo: DebugInfo
) {
    lateinit var parent: IrModuleHeader

    override fun toString(): String {
        return "fun $name(${arguments.mapIndexed { i, t -> "%$i : $t" }.joinToString(",")})" +
                " : $returnType (${permissions.joinToString(" ")})"
    }

    // Used specifically in the LLVM Backend for checking imported functions
    override fun equals(other: Any?): Boolean {
        return other is IrFunctionHeader
                && other.name == this.name
                && other.arguments == this.arguments
                && other.returnType == this.returnType
                && other.permissions == this.permissions
    }

    override fun hashCode(): Int {
        return Arrays.deepHashCode(arrayOf(name, returnType, *arguments.toTypedArray(), *permissions.toTypedArray()))
    }

}

class IrExternalFunction(
        name: String,
        returnType: Type,
        arguments: List<Type>,
        permissions: List<Permission>,
        debugInfo: DebugInfo
) : IrFunctionHeader(name, returnType, arguments, permissions, debugInfo) {
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
) : IrFunctionHeader(name, returnType, registerTypes.subList(0, argumentCount), permissions, debugInfo) {
    override fun toString(): String {
        return "fun $name(${arguments.mapIndexed { i, t -> "%$i : $t" }.joinToString(",")})" +
                " : $returnType (${permissions.joinToString(" ")})\n" +
                instructions.joinToString("\n") { "    $it" }
    }
}

class IrStruct(
        name: String,
        val fields: List<Type>,
        val debugInfo: DebugInfo
) : Type(name) {

//    override fun toString(): String = "struct $name { ${fields.joinToString(", ")} }"

    override fun equals(other: Any?): Boolean {
        return other is IrStruct
                && other.name == this.name
                && other.fields == this.fields
    }

}

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
        val function: IrFunctionHeader,
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
