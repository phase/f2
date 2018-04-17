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
            val outputFiles = it.getOutputFiles()
            val errOut = outputFiles.find { it.extension == "err" }

            if (errOut != null) {
                DynamicTest.dynamicTest(it.name) {
                    when (it) {
                        is SingleSourceTest -> {
                            val expectedError = errOut.readText().removeSuffix("\n")

                            val timer = System.nanoTime()
                            val ir = compile(it.name, it.source)
                            val time = ((System.nanoTime() - timer).toDouble()) / 1000000000.0

                            val errors = ir.errors.joinToString("\n\n").removeSuffix("\n")
                            assertTrue(expectedError.contains(errors),
                                    "$paddedName.${RED_COLOR}FAILED$RESET_COLOR\n\n" +
                                            "Expected Errors:\n\n" +
                                            "$expectedError\n\n" +
                                            "Actual Errors:\n\n" +
                                            "$errors\n\n")
                            println("$paddedName.${GREEN_COLOR}ok$RESET_COLOR ${time}s")
                        }
                        is MultipleSourceTest -> {
                            // TODO
                            assertTrue(false,
                                    "$paddedName.${RED_COLOR}FAILED$RESET_COLOR\n\n" +
                                            "Multiple Source Error Tests aren't implemented yet.")
                        }
                    }
                }
            } else null
        }.filterNotNull()
    }
}
