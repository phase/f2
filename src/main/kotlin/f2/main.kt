package f2

import f2.ast.ASTBuilder
import f2.ast.AstModule
import f2.backend.Backend
import f2.backend.llvm.LLVMBackend
import f2.ir.IrModule
import f2.ir.convert
import f2.ir.pass.optimize.HeapToStackPass
import f2.ir.pass.semantics.MemoryValidatorPass
import f2.parser.LangLexer
import f2.parser.LangParser
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import java.io.File

fun main(args: Array<String>) {
    val code = """
struct X {
  a : Int32
}
struct Y {
  x : X
}

internal_add_i32 :: Int32 -> Int32 -> Int32 +External

add :: Int32 -> Int32 -> Int32
add x y = internal_add_i32(x, y).

f :: X -> Int32
f x = x.a.

g :: Int32 -> X
g a = X{a}.

h :: Int32 -> Int32
h a = let x = X{a},
      x.a.

i :: Int32 -> Y
i a = Y{X{a}}.

j :: Int32 -> Int32
j a = let x = X{a},
      let y = Y{x},
      let w = y.x,
      w.a.
"""
    println(compileLLVM(astToIr(stringToAst("parser_test", code))))
}

fun stringToAst(moduleName: String, code: String): AstModule {
    val stream = ANTLRInputStream(code)
    val lexer = LangLexer(stream)
    val tokens = CommonTokenStream(lexer)
    val parser = LangParser(tokens)
    val result = parser.module()
    val astBuilder = ASTBuilder(moduleName, code)
    return astBuilder.visitModule(result)
}

fun astToIr(astModule: AstModule): IrModule {
    var ir = convert(astModule)

    val passes: List<(IrModule) -> IrModule> = listOf(
            { i -> HeapToStackPass(i).optimize() },
            { i -> MemoryValidatorPass(i).optimize() }
    )
    passes.forEach { ir = it(ir) }

    return ir
}

fun compileLLVM(irModule: IrModule): String {
    val file = File.createTempFile("llvm", "out.ll")
    val backend = LLVMBackend(irModule)
    backend.output(file)
    return file.readText()
}
