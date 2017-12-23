package f2.ir.pass.optimize

import f2.ast.DebugInfo
import f2.ir.*
import f2.ir.pass.Pass
import f2.type.Type
import java.util.*

class FreePass(irModule: IrModule) : Pass(irModule) {

    override fun optimizeInstructions(
            irFunction: IrFunction,
            registers: List<Type>,
            instructions: List<Instruction>
    ): Pair<List<Type>, List<Instruction>> {
        val regs: MutableList<Type> = registers.toMutableList()
        var ins: MutableList<Instruction> = instructions.toMutableList()

        val functionCallsThatReturnAllocations = Stack<Pair<Int/*index in ins*/, FunctionCallInstruction>>()
        val allocationsFromBelow = mutableMapOf<Int/*reg*/, Int/*index in ins*/>()
        val lastPlaceAllocationWasUsed = mutableMapOf<Int/*reg*/, Int/*index in ins*/>()

        ins.forEachIndexed { index, instruction ->
            if (instruction is FunctionCallInstruction) {
                // Index arguments that are allocations
                val argumentsThatAreAllocations = instruction.registerIndexes.filter { regs[it] is IrStruct }
                argumentsThatAreAllocations.forEach {
                    if (allocationsFromBelow.containsKey(it)) {
                        lastPlaceAllocationWasUsed.put(it, index)
                    }
                }

                // Assume the function isn't null
                // TODO: Add pass that finds these and reports error
                val functionCalled = irModule.getFunction(instruction.functionName)!!
                if (functionCalled.returnType is IrStruct) {
                    // the function call returned a heap allocation that needs to be freed
                    functionCallsThatReturnAllocations.push(Pair(index, instruction))
                }
            }

            // TODO: The semantics for this need to be fleshed out
            if (instruction is FieldSetInstruction) {
                val possibleAllocationRegister = instruction.valueRegisterIndex
                if (regs[possibleAllocationRegister] is IrStruct
                        && allocationsFromBelow.containsKey(possibleAllocationRegister)) {
                    lastPlaceAllocationWasUsed.put(possibleAllocationRegister, index)
                }
            }

            if (instruction is StoreInstruction) {
                if (!functionCallsThatReturnAllocations.empty()) {
                    val v = functionCallsThatReturnAllocations.pop()
                    allocationsFromBelow.put(instruction.register, v.first)
                    lastPlaceAllocationWasUsed.put(instruction.register, index)
                }
            }
        }

        lastPlaceAllocationWasUsed.forEach { register, index ->
            val free = FreeAllocationInstruction(ins[index].debugInfo(), register)
            val newInstructions = ins.toList().subList(0, index + 1).toMutableList()
            newInstructions.add(free)
            newInstructions.addAll(ins.subList(index + 1, ins.size))
            ins = newInstructions
        }

        return Pair(regs, ins)
    }
}
