package f2.backend

import f2.ir.IrModule
import org.bytedeco.javacpp.*
import org.bytedeco.javacpp.LLVM.*
import java.io.File
import java.nio.charset.Charset

class LLVMBackend(irModule: IrModule) : Backend(irModule) {

    val llvmModule: LLVM.LLVMModuleRef
    var error = BytePointer(null as Pointer?)
    val targetMachine: LLVMTargetMachineRef

    val context: LLVMContextRef
    val builder: LLVMBuilderRef

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

}
