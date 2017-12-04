package f2.ir.optimize.pass

import f2.ast.Type
import f2.ir.Instruction
import f2.ir.IrFunction
import f2.ir.IrModule

abstract class Pass(val irModule: IrModule) {

    fun optimize(): IrModule {
        val functions = irModule.functions.map { optimize(it) }
        return IrModule(irModule.name, functions, irModule.structs)
    }

    fun optimize(irFunction: IrFunction): IrFunction {
        val optimizedInstructions: Pair<List<Type>, List<Instruction>> =
                optimizeInstructions(irFunction, irFunction.registerTypes, irFunction.instructions)

        return IrFunction(irFunction.name, irFunction.returnType, irFunction.argumentCount,
                optimizedInstructions.first, optimizedInstructions.second)
    }

    abstract fun optimizeInstructions(
            irFunction: IrFunction,
            registers: List<Type>,
            instructions: List<Instruction>
    ): Pair<List<Type>, List<Instruction>>

}
