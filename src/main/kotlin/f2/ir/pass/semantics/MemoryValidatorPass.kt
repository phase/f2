package f2.ir.pass.semantics

import f2.ast.reportError
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
                    reportError(irModule.source, it.debugInfo, "Trying to modify arguments without +Mutable")
                }
                if (it.valueRegisterIndex < irFunction.argumentCount && registers[it.valueRegisterIndex] is IrStruct) {
                    reportError(irModule.source, it.debugInfo, "Trying to store argument pointer into a local struct")
                }
            }
        }

        return Pair(registers, instructions)
    }

}
