package f2.test.suites

import f2.astToIr
import f2.compileLLVM
import f2.ir.IrModule
import f2.stringToAst
import f2.test.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertTrue

object LLVMTest {

    @BeforeAll
    @JvmStatic
    fun beforeAllTests() {
        println("-- LLVM Tests --\n")
    }

    private fun compile(moduleName: String, source: String): IrModule {
        return astToIr(stringToAst(moduleName, source))
    }

    @TestFactory
    fun llvmTests(): List<DynamicTest> {
        return Tests.pmap {
            val id = it.id.toString().padStart(4, '0')
            val paddedName = "#$BLUE_COLOR$id$RESET_COLOR \"${it.name}\"".padEnd(63, '.')
            val outputFiles = getOutputFiles(it.id)
            val llvmOut = outputFiles.filter { it.extension == "ll" }.firstOrNull()

            if (llvmOut != null) {
                DynamicTest.dynamicTest(it.name) {
                    val expectedOutput = llvmOut.readText().removeSuffix("\n")
                    val ir = compile(it.name, it.source)
                    val errors = ir.errors.joinToString("\n")
                    assertTrue(ir.errors.size == 0,
                            "$paddedName.${RED_COLOR}FAILED$RESET_COLOR\n\n" +
                                    "Expected 0 errors.\n\n" +
                                    "Actual:\n\n" +
                                    "$errors\n\n")

                    val actualOutput = compileLLVM(ir).removeSuffix("\n")
                    assertTrue(actualOutput.contains(expectedOutput),
                            "$paddedName.${RED_COLOR}FAILED$RESET_COLOR\n\n" +
                                    "Expected:\n\n" +
                                    "$expectedOutput\n\n" +
                                    "Actual:\n\n" +
                                    "$actualOutput\n\n")
                    println("$paddedName.${GREEN_COLOR}ok$RESET_COLOR")
                }

            } else null
        }.filterNotNull()
    }
}
