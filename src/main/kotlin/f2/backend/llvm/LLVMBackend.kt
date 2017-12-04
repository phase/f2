package f2.backend.llvm

import f2.type.*
import f2.backend.Backend
import f2.ir.IrExternalFunction
import f2.ir.IrFunction
import f2.ir.IrModule
import f2.ir.IrStruct
import org.bytedeco.javacpp.*
import org.bytedeco.javacpp.LLVM.*
import java.io.File

class LLVMBackend(irModule: IrModule) : Backend(irModule) {

    val llvmModule: LLVM.LLVMModuleRef
    var error = BytePointer(null as Pointer?)
    val targetMachine: LLVMTargetMachineRef

    val context: LLVMContextRef
    val builder: LLVMBuilderRef

    val functions = mutableMapOf<String, LLVMValueRef>()
    val structTypes = mutableMapOf<String, LLVMTypeRef>()

    init {
        LLVMLinkInMCJIT()
        LLVMInitializeNativeAsmPrinter()
        LLVMInitializeNativeAsmParser()
        LLVMInitializeNativeDisassembler()
        LLVMInitializeNativeTarget()

        context = LLVMContextCreate()
        llvmModule = LLVMModuleCreateWithNameInContext(irModule.name, context)
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
        val malloc = LLVMAddFunction(llvmModule, "malloc", mallocType)

        // Declare Free
        val freeType: LLVMTypeRef = LLVMFunctionType(LLVMVoidTypeInContext(context),
                PointerPointer<LLVMTypeRef>(*arrayOf(LLVMPointerType(LLVMInt8TypeInContext(context), 0))), 1, 0)
        val free = LLVMAddFunction(llvmModule, "free", freeType)

        generate()
    }

    override fun output(file: File?) {
        LLVMVerifyModule(llvmModule, LLVMAbortProcessAction, error)
        LLVMDisposeMessage(error)

        val pass = LLVMCreatePassManager()
        LLVMAddConstantPropagationPass(pass)
        LLVMAddInstructionCombiningPass(pass)
        LLVMAddPromoteMemoryToRegisterPass(pass)
        LLVMAddGVNPass(pass)
        LLVMAddCFGSimplificationPass(pass)
        LLVMRunPassManager(pass, llvmModule)

        if (file != null) {
            // Print out LLVM IR
            error = BytePointer(null as Pointer?)
            LLVMPrintModuleToFile(llvmModule, file.path, error)
            LLVMDisposeMessage(error)
        }

        LLVMDisposeBuilder(builder)
        LLVMContextDispose(context)
        LLVMDisposePassManager(pass)
    }

    fun generate() {
        irModule.externalFunctions.forEach { generate(it) }
        irModule.functions.forEach { generate(it) }
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
        functions.put(irExternalFunction.name, function)
    }

    fun generate(irFunction: IrFunction) {

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
                    val fieldTypes = type.fields.map {
                        if (it is IrStruct) {
                            LLVMPointerType(getLLVMType(it), 0)
                        } else {
                            getLLVMType(it)
                        }
                    }
                    val llvmStructType = LLVMStructCreateNamed(context, type.name)
                    LLVMStructSetBody(llvmStructType, PointerPointer(*fieldTypes.toTypedArray()), type.fields.size, 0)
                    structTypes.put(type.name, llvmStructType)
                    llvmStructType
                }
            }
            else -> throw Exception("Can't find type $type")
        }
    }


}
