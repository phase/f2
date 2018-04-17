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

    const val LLVM_EXTENSION = "ll"

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
            val outputFiles = it.getOutputFiles()
            val llvmOut = outputFiles.find { it.extension == LLVM_EXTENSION }

            if (llvmOut != null) {
                DynamicTest.dynamicTest(it.name) {
                    when (it) {
                        is SingleSourceTest -> {
                            val expectedOutput = llvmOut.readText().removeSuffix("\n")

                            val timer = System.nanoTime()
                            val ir = compile(it.name, it.source)
                            val time = ((System.nanoTime() - timer).toDouble()) / 1000000000.0

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

                            println("$paddedName.${GREEN_COLOR}ok$RESET_COLOR ${time}s")
                        }
                        is MultipleSourceTest -> {
                            val expectedOutputs = outputFiles.filter { it.extension == LLVM_EXTENSION }
                                    .pmap { it.nameWithoutExtension to it.readText() }

                            val timer = System.nanoTime()
                            val ir = astToIr(it.sources.map { stringToAst(it.key, it.value) })
                            val time = ((System.nanoTime() - timer).toDouble()) / 1000000000.0

                            val errors = ir.map { Triple(it.name, it.errors.size, it.errors.joinToString("\n")) }

                            val errorString = errors.joinToString("\n\n") {
                                "Errors in module ${it.first.joinToString(".")}:\n\n${it.third}"
                            }

                            assertTrue(errors.foldRight(0) { triple, acc ->
                                acc + triple.second
                            } == 0,
                                    "$paddedName.${RED_COLOR}FAILED$RESET_COLOR\n\n" +
                                            "Expected 0 errors.\n\n" +
                                            "Actual:\n\n" +
                                            "$errorString\n\n")

                            val actualOutputs = ir.map { it.name.joinToString(".") to compileLLVM(it).removeSuffix("\n") }

                            actualOutputs.forEach { actualOutput ->
                                val expectedOutput = expectedOutputs.find { it.first == actualOutput.first }

                                assertTrue(expectedOutput != null,
                                        "$paddedName.${RED_COLOR}FAILED$RESET_COLOR\n\n" +
                                                "Can't find expected output file for ${actualOutput.first}")
                                expectedOutput!!

                                assertTrue(actualOutput.second.trim().contains(expectedOutput.second.trim()),
                                        "$paddedName.${RED_COLOR}FAILED$RESET_COLOR\n\n" +
                                                "Expected:\n\n" +
                                                "${expectedOutput.second}\n\n" +
                                                "Actual:\n\n" +
                                                "${actualOutput.second}\n\n")
                            }

                            println("$paddedName.${GREEN_COLOR}ok$RESET_COLOR ${time}s")
                        }
                    }
                }
            } else null
        }.filterNotNull()
    }
}
