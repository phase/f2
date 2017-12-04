package f2.ir.optimize.pass

import f2.ast.Type
import f2.ir.*
import java.util.*

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
                if (regs[instruction.registerIndex] is IrStruct) {
                    allocationsNotSeen.remove(instruction.registerIndex)
                }
            }
        }

        allocationsNotSeen.forEach {
            val insIndex = heapInstructionLocation[it]
            insIndex?.let {
                val currentIns = ins[insIndex]
                if (currentIns is HeapAllocateInstruction) {
                    val type = currentIns.type
                    ins[insIndex] = StackAllocateInstruction(type)
                }
            }
        }

        return Pair(regs, ins)
    }

}
