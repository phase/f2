package f2.ir.pass

import f2.ir.*
import f2.type.Type

abstract class Pass(val irModule: IrModule) {

    fun optimize(): IrModule {
        // replace functions in IrModule with optimized versions
        val optimizedFunctions = irModule.functions.filterIsInstance<IrFunction>().map { optimize(it) }
        val functions: MutableList<IrFunctionHeader> = irModule.functions.filterIsInstance<IrExternalFunction>().toMutableList()
        functions.addAll(optimizedFunctions)
        return IrModule(irModule.name, irModule.structs, functions, irModule.imports, irModule.source, irModule.errors)
    }

    fun optimize(irFunction: IrFunction): IrFunction {
        val optimizedInstructions: Pair<List<Type>, List<Instruction>> =
                optimizeInstructions(irFunction, irFunction.registerTypes, irFunction.instructions)

        return IrFunction(irFunction.name, irFunction.returnType, irFunction.permissions, irFunction.argumentCount,
                optimizedInstructions.first, optimizedInstructions.second, irFunction.debugInfo)
    }

    abstract fun optimizeInstructions(
            irFunction: IrFunction,
            registers: List<Type>,
            instructions: List<Instruction>
    ): Pair<List<Type>, List<Instruction>>

}
