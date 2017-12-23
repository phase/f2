package f2.ir.pass.optimize

import f2.ir.Instruction
import f2.ir.IrFunction
import f2.ir.IrModule
import f2.ir.pass.Pass
import f2.type.Type

class FreePass(irModule: IrModule) : Pass(irModule) {

    override fun optimizeInstructions(
            irFunction: IrFunction,
            registers: List<Type>,
            instructions: List<Instruction>
    ): Pair<List<Type>, List<Instruction>> {
        val regs: MutableList<Type> = registers.toMutableList()
        val ins: MutableList<Instruction> = instructions.toMutableList()

        // TODO Algorithm

        return Pair(regs, ins)
    }
}
