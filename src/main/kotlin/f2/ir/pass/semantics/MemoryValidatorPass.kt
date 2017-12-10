package f2.ir.pass.semantics

import f2.ir.*
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
                if (it.structRegisterIndex < irFunction.argumentCount && registers[it.structRegisterIndex] is IrStruct) {
                    irModule.error(it.debugInfo, "Trying to modify arguments without +Mutable")
                }
                if (it.valueRegisterIndex < irFunction.argumentCount && registers[it.valueRegisterIndex] is IrStruct) {
                    irModule.error(it.debugInfo, "Trying to store argument pointer into a local struct")
                }
            }
            if (it is ReturnInstruction) {
                if (it.registerIndex < irFunction.argumentCount && registers[it.registerIndex] is IrStruct) {
                    irModule.error(it.debugInfo, "Trying to return argument pointer")
                }
            }
        }

        return Pair(registers, instructions)
    }

}
