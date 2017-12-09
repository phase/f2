package f2.ir.pass.optimize

import f2.type.Type
import f2.ir.*
import f2.ir.pass.Pass

/**
 * Translates HeapAllocation instructions to StackAllocation Instructions
 */
class HeapToStackPass(irModule: IrModule) : Pass(irModule) {

    override fun optimizeInstructions(
            irFunction: IrFunction,
            registers: List<Type>,
            instructions: List<Instruction>
    ): Pair<List<Type>, List<Instruction>> {
        val regs: MutableList<Type> = registers.toMutableList()
        val ins: MutableList<Instruction> = instructions.toMutableList()

        // the algorithm
        // "seen" means the allocation goes above the function
        // "going above the function" means is returned

        var lastValue: Pair<Int, ValueInstruction>? = null
        val heapInstructionLocation: MutableMap<Int/*reg*/, Int/*index in ins*/> = mutableMapOf()
        val allocationsNotSeen = mutableListOf<Int/*reg*/>()
        val structReliance = mutableMapOf<Int/*reg*/, MutableList<Int/*reg*/>>()

        ins.forEachIndexed { index, instruction ->
            if (instruction is HeapAllocateInstruction) {
                lastValue = Pair(index, instruction)
            }

            if (instruction is StoreInstruction) {
                lastValue?.let {
                    if (it.second is HeapAllocateInstruction) {
                        val r = instruction.register
                        allocationsNotSeen.add(r)
                        heapInstructionLocation.put(r, it.first)
                    }
                }
            }

            if (instruction is ReturnInstruction) {
                val reg = instruction.registerIndex
                if (regs[reg] is IrStruct) {
                    if (allocationsNotSeen.contains(reg)) {
                        allocationsNotSeen.remove(reg)
                    }
                    structReliance[reg]?.forEach {
                        if (allocationsNotSeen.contains(it)) {
                            allocationsNotSeen.remove(it)
                        }
                    }
                }
            }

            if (instruction is FieldSetInstruction) {
                if (regs[instruction.valueRegisterIndex] is IrStruct) {
                    if (!structReliance.containsKey(instruction.structRegisterIndex)) {
                        structReliance[instruction.structRegisterIndex] = mutableListOf()
                    }
                    structReliance[instruction.structRegisterIndex]?.add(instruction.valueRegisterIndex)
                }
            }
        }

        allocationsNotSeen.forEach {
            val insIndex = heapInstructionLocation[it]
            insIndex?.let {
                val currentIns = ins[insIndex]
                if (currentIns is HeapAllocateInstruction) {
                    val type = currentIns.type
                    ins[insIndex] = StackAllocateInstruction(currentIns.debugInfo, type)
                }
            }
        }

        return Pair(regs, ins)
    }

}
