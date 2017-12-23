package f2.test.suites

import f2.astToIr
import f2.ir.IrModule
import f2.stringToAst
import f2.test.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertTrue

object ErroringTests {

    @BeforeAll
    @JvmStatic
    fun beforeAllTests() {
        println("-- Error Tests --\n")
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
            val errOut = outputFiles.filter { it.extension == "err" }.firstOrNull()

            if (errOut != null) {
                DynamicTest.dynamicTest(it.name) {
                    val expectedError = errOut.readText().removeSuffix("\n")
                    val ir = compile(it.name, it.source)
                    val errors = ir.errors.joinToString("\n\n").removeSuffix("\n")
                    assertTrue(expectedError.contains(errors),
                            "$paddedName.${RED_COLOR}FAILED$RESET_COLOR\n\n" +
                                    "Expected Errors:\n\n" +
                                    "$expectedError\n\n" +
                                    "Actual Errors:\n\n" +
                                    "$errors\n\n")
                    println("$paddedName.${GREEN_COLOR}ok$RESET_COLOR")
                }
            } else null
        }.filterNotNull()
    }
}
