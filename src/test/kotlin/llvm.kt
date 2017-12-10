import f2.astToIr
import f2.compileLLVM
import f2.ir.IrModule
import f2.stringToAst
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertTrue

class LLVMTest {
    private fun compile(moduleName: String, source: String): IrModule {
        return astToIr(stringToAst(moduleName, source))
    }

    @TestFactory
    fun llvmTests(): List<DynamicTest> {
        println("this piece of shit is running")
        return Tests.pmap {
            val id = it.id.toString().padStart(4, '0')
            val outputFiles = getOutputFiles(it.id)
            val llvmOut = outputFiles.filter { it.extension == "ll" }.firstOrNull()
            val errOut = outputFiles.filter { it.extension == "err" }.firstOrNull()

            if (llvmOut != null) {
                DynamicTest.dynamicTest(it.name) {
                    val expectedOutput = llvmOut.readText().removeSuffix("\n")
                    val ir = compile(it.name, it.source)
                    val errors = ir.errors.joinToString("\n")
                    assertTrue(ir.errors.size == 0,
                            "Test #$id FAILED." +
                                    "\nExpected 0 errors." +
                                    "\nActual:\n" +
                                    errors)

                    val actualOutput = compileLLVM(ir).removeSuffix("\n")
                    assertTrue(actualOutput.contains(expectedOutput),
                            "Test #$id FAILED.\n" +
                                    "Expected:\n" +
                                    "$expectedOutput\n" +
                                    "Actual:\n" +
                                    "$actualOutput\n")
                    println("Test #$id PASSED.")
                }
            } else if (errOut != null) {
                DynamicTest.dynamicTest(it.name) {
                    val expectedError = errOut.readText().removeSuffix("\n")
                    val ir = compile(it.name, it.source)
                    val errors = ir.errors.joinToString("\n").removeSuffix("\n")
                    assertTrue(expectedError.contains(errors),
                            "Test #$id FAILED.\n\n" +
                                    "Expected Errors:\n\n" +
                                    "$expectedError\n\n" +
                                    "Actual Errors:\n\n" +
                                    "$errors\n")
                    println("Test #$id PASSED.")
                }
            } else null
        }.filterNotNull()
    }
}
