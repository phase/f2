package f2.ir.pass

import f2.type.Type
import f2.ir.Instruction
import f2.ir.IrFunction
import f2.ir.IrModule

abstract class Pass(val irModule: IrModule) {

    fun optimize(): IrModule {
        val functions = irModule.functions.map { optimize(it) }
        return IrModule(irModule.name, irModule.externalFunctions, functions, irModule.structs,
                irModule.source, irModule.errors)
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
