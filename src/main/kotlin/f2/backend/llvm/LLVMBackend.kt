package f2.backend.llvm

import f2.type.*
import f2.backend.Backend
import f2.ir.*
import org.bytedeco.javacpp.*
import org.bytedeco.javacpp.LLVM.*
import java.io.File
import java.util.*

class LLVMBackend(irModule: IrModule) : Backend(irModule) {

    val llvmModule: LLVM.LLVMModuleRef
    var error = BytePointer(null as Pointer?)
    val targetMachine: LLVMTargetMachineRef

    val context: LLVMContextRef
    val builder: LLVMBuilderRef

    val functions = mutableMapOf<IrFunctionHeader, LLVMValueRef>()
    val structTypes = mutableMapOf<String, LLVMTypeRef>()

    // TODO: Move to stdlib
    val malloc: LLVMValueRef
    val free: LLVMValueRef

    init {
        LLVMLinkInMCJIT()
        LLVMInitializeNativeAsmPrinter()
        LLVMInitializeNativeAsmParser()
        LLVMInitializeNativeDisassembler()
        LLVMInitializeNativeTarget()

        context = LLVMContextCreate()
        llvmModule = LLVMModuleCreateWithNameInContext(irModule.name.joinToString("."), context)
        builder = LLVMCreateBuilder()

        val targetTriple = LLVMGetDefaultTargetTriple()
        LLVMSetTarget(llvmModule, targetTriple)

        val target = LLVMTargetRef(null as BytePointer?)
        val error = BytePointer(null as Pointer?)
        LLVMGetTargetFromTriple(targetTriple, target, error)

        LLVMDisposeMessage(error)

        targetMachine = LLVMCreateTargetMachine(target, targetTriple.string, "", "", 0, 0, 0)
        LLVMDisposeMessage(targetTriple)

        // Declare Malloc
        val mallocType: LLVMTypeRef = LLVMFunctionType(LLVMPointerType(LLVMInt8TypeInContext(context), 0),
                PointerPointer<LLVMTypeRef>(*arrayOf(LLVMInt64TypeInContext(context))), 1, 0)
        malloc = LLVMAddFunction(llvmModule, "malloc", mallocType)

        // Declare Free
        val freeType: LLVMTypeRef = LLVMFunctionType(LLVMVoidTypeInContext(context),
                PointerPointer<LLVMTypeRef>(*arrayOf(LLVMPointerType(LLVMInt8TypeInContext(context), 0))), 1, 0)
        free = LLVMAddFunction(llvmModule, "free", freeType)

        generate()
    }

    override fun output(file: File?) {
//        LLVMVerifyModule(llvmModule, LLVMAbortProcessAction, error)
//        LLVMDisposeMessage(error)

        val pass = LLVMCreatePassManager()
        LLVMAddConstantPropagationPass(pass)
        LLVMAddInstructionCombiningPass(pass)
        LLVMAddPromoteMemoryToRegisterPass(pass)
        LLVMAddGVNPass(pass)
        LLVMAddCFGSimplificationPass(pass)
//        LLVMRunPassManager(pass, llvmModule)

        if (file != null) {
            // Print out LLVM IR
            error = BytePointer(null as Pointer?)
            LLVMPrintModuleToFile(llvmModule, file.path, error)
            LLVMDisposeMessage(error)
        }

//        LLVMDumpModule(llvmModule)

        LLVMDisposeBuilder(builder)
        LLVMContextDispose(context)
        LLVMDisposePassManager(pass)
    }

    fun generate() {
        irModule.functions.filterIsInstance<IrExternalFunction>().forEach { generate(it) }
        irModule.functions.filterIsInstance<IrFunction>().forEach { generate(it) }
    }

    fun generate(irExternalFunction: IrExternalFunction) {
        val returnType = getLLVMType(irExternalFunction.returnType)
        val argumentTypes = irExternalFunction.arguments.map { getLLVMType(it) }
        val functionType = LLVMFunctionType(
                returnType,
                PointerPointer<LLVMTypeRef>(*argumentTypes.toTypedArray()),
                argumentTypes.size,
                0
        )
        val function: LLVMValueRef = LLVMAddFunction(llvmModule, irExternalFunction.name, functionType)
        LLVMSetFunctionCallConv(function, LLVMCCallConv)
        functions.put(irExternalFunction, function)
    }

    fun generate(irFunction: IrFunction) {
        // Setup the function
        val returnType = getLLVMType(irFunction.returnType)
        val argumentTypes = irFunction.registerTypes.subList(0, irFunction.argumentCount).map { getLLVMType(it) }
        val functionType = LLVMFunctionType(
                returnType,
                PointerPointer<LLVMTypeRef>(*argumentTypes.toTypedArray()),
                argumentTypes.size,
                0
        )
        val function: LLVMValueRef = LLVMAddFunction(llvmModule, irFunction.name, functionType)
        LLVMSetFunctionCallConv(function, LLVMCCallConv)
        functions.put(irFunction, function)

        val entryBlock = LLVMAppendBasicBlock(function, "entry")
        LLVMPositionBuilderAtEnd(builder, entryBlock)

        // Generate the IR

        val valueStack = Stack<LLVMValueRef>()
        val registerValueRefs = mutableMapOf<Int, LLVMValueRef>()

        (0 until irFunction.argumentCount).forEach { registerValueRefs.put(it, LLVMGetParam(function, it)) }

        fun generate(i: Instruction, registers: List<Type>) {
            when (i) {
                is StoreInstruction -> {
                    registerValueRefs.put(i.register, valueStack.pop())
                }
                is FunctionCallInstruction -> {
                    val f = functions[i.function]
                    val arguments = i.registerIndexes.map { registerValueRefs[it]!! }
                    val call = if (f != null) {
                        LLVMBuildCall(builder, f, PointerPointer(*arguments.toTypedArray()), arguments.size, "")
                    } else {
                        // the function being called is imported from another module,
                        // so we want to declare it as external and make an llvm function call
                        // to the external function
                        val externalIrFunction = i.function
                        val externalReturnType = getLLVMType(externalIrFunction.returnType)
                        val externalArgumentTypes = externalIrFunction.arguments.map { getLLVMType(it) }
                        val externalFunctionType = LLVMFunctionType(
                                externalReturnType,
                                PointerPointer<LLVMTypeRef>(*externalArgumentTypes.toTypedArray()),
                                externalArgumentTypes.size,
                                0
                        )
                        val externalFunction: LLVMValueRef = LLVMAddFunction(llvmModule, externalIrFunction.name, externalFunctionType)
                        LLVMSetFunctionCallConv(externalFunction, LLVMCCallConv)
                        functions.put(externalIrFunction, externalFunction)

                        // now that we have created the function, call it
                        LLVMBuildCall(builder, externalFunction, PointerPointer(*arguments.toTypedArray()), arguments.size, "")
                    }
                    valueStack.push(call)
                }
                is ReturnInstruction -> {
                    LLVMBuildRet(builder, registerValueRefs[i.registerIndex]!!)
                }
                is FieldGetInstruction -> {
                    val struct = registerValueRefs[i.registerIndex]!!
                    val gep = LLVMBuildStructGEP(builder, struct, i.fieldIndex, "")
                    val load = LLVMBuildLoad(builder, gep, "")
                    valueStack.push(load)
                }
                is FieldSetInstruction -> {
                    val struct = registerValueRefs[i.structRegisterIndex]!!
                    val gep = LLVMBuildStructGEP(builder, struct, i.fieldIndex, "")
                    val value = registerValueRefs[i.valueRegisterIndex]!!
                    LLVMBuildStore(builder, value, gep)
                }
                is StackAllocateInstruction -> {
                    val struct = LLVMGetElementType(getLLVMType(i.type))
                    valueStack.push(LLVMBuildAlloca(builder, struct, ""))
                }
                is HeapAllocateInstruction -> {
                    val type = getLLVMType(i.type)
                    val size = sizeOfStruct(i.type)

                    val mem = LLVMBuildCall(
                            builder,
                            malloc,
                            PointerPointer<LLVMValueRef>(
                                    *arrayOf(LLVMConstInt(LLVMInt64TypeInContext(context), size, 0))),
                            1, ""
                    )
                    val bitCast = LLVMBuildBitCast(builder, mem, type, "")
                    valueStack.push(bitCast)
                }
                is FreeAllocationInstruction -> {
                    val struct = registerValueRefs[i.register]!!
                    val bitPointer = LLVMBuildBitCast(
                            builder,
                            struct,
                            LLVMPointerType(LLVMInt8TypeInContext(context), 0),
                            "")
                    LLVMBuildCall(
                            builder,
                            free,
                            PointerPointer<LLVMValueRef>(*arrayOf(bitPointer)),
                            1, "")
                }
            }
        }

        irFunction.instructions.forEach {
            generate(it, irFunction.registerTypes)
        }
    }

    fun getLLVMType(type: Type): LLVMTypeRef {
        return when (type) {
            is Int8 -> LLVMInt8TypeInContext(context)
            is Int16 -> LLVMInt16TypeInContext(context)
            is Int32 -> LLVMInt32TypeInContext(context)
            is Int64 -> LLVMInt64TypeInContext(context)
            is Float32 -> LLVMFloatTypeInContext(context)
            is Float64 -> LLVMDoubleTypeInContext(context)
            is IrStruct -> {
                if (structTypes.containsKey(type.name)) {
                    structTypes[type.name]!!
                } else {
                    val fieldTypes = type.fields.map { getLLVMType(it) }
                    val llvmStructType = LLVMStructCreateNamed(context, type.name)
                    LLVMStructSetBody(llvmStructType, PointerPointer(*fieldTypes.toTypedArray()), type.fields.size, 0)
                    val structPointer = LLVMPointerType(llvmStructType, 0)
                    structTypes.put(type.name, structPointer)
                    structPointer
                }
            }
            else -> throw Exception("Can't find type $type {${type.javaClass}}")
        }
    }

    fun sizeOfStruct(irStruct: IrStruct): Long = irStruct.fields.map {
        when (it) {
            is Int8 -> 1L
            is Int16 -> 2L
            is Int32 -> 4L
            is Int64 -> 8L
            is Float32 -> 4L
            is Float64 -> 8L
            is IrStruct -> 8L
            else -> throw Exception("Can't find type $it")
        }
    }.sum()


}
