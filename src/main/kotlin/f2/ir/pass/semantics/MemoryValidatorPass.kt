package f2.ir.pass.semantics

import f2.ir.FieldSetInstruction
import f2.ir.Instruction
import f2.ir.IrFunction
import f2.ir.IrModule
import f2.ir.pass.Pass
import f2.type.Type

class MemoryValidatorPass(irModule: IrModule) : Pass(irModule) {

    override fun optimizeInstructions(
            irFunction: IrFunction,
            registers: List<Type>,
            instructions: List<Instruction>
    ): Pair<List<Type>, List<Instruction>> {

        instructions.forEach {
            if (it is FieldSetInstruction) {
                if (it.structRegisterIndex < irFunction.argumentCount) {
                    // TODO Error: trying to modify arguments
                }
                if (it.valueRegisterIndex < irFunction.argumentCount) {
                    // TODO Error: trying to store argument pointer into a local struct
                }
            }
        }

        return Pair(registers, instructions)
    }

}
