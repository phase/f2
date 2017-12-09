package f2

import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.RuleContext
import f2.ast.*
import f2.backend.llvm.LLVMBackend
import f2.ir.IrModule
import f2.ir.convert
import f2.ir.pass.optimize.HeapToStackPass
import f2.parser.LangLexer
import f2.parser.LangParser
import f2.type.Int32
import java.io.File

fun main(args: Array<String>) {
    val X = AstStruct("X", listOf(AstField("a", Int32)), listOf(), listOf(), listOf())
    val Y = AstStruct("Y", listOf(AstField("x", X)), listOf(), listOf(), listOf())
    /*
    struct X {
      let a : Int
    }
    struct Y {
      let x : X
    }
    internal_add_i32 :: Int -> Int -> Int
    add :: Int -> Int -> Int
    add x y =
        let z = internal_add_i32(x, y),
        let w = internal_add_i32(internal_add_i32(x, y), z),
        return w.

    f :: X -> Int
    f x = x.a.
    f x = let z = x.a, return z.

    g :: Int -> X
    g a = new X(a).

    h :: Int -> Int
    h a = let x = new X(a),
          let r = x.a,
          return a.

    i :: Int32 -> Y
    i a = let x = new X(a),
          let y = new Y(x),
          return y.

    j :: Int32 -> Int32
    j a = let x = new X(a),
          let y = new Y(x),
          let w = y.x,
          let b = w.a,
          return b.
     */
    val program: AstModule = AstModule(
            "addition",
            listOf(
                    AstFunctionDeclaration("internal_add_i32", listOf(Int32, Int32), Int32, listOf()),
                    AstFunctionDeclaration("add", listOf(Int32, Int32), Int32, listOf()),
                    AstFunctionDeclaration("f", listOf(X), Int32, listOf()),
                    AstFunctionDeclaration("g", listOf(Int32), X, listOf()),
                    AstFunctionDeclaration("h", listOf(Int32), Int32, listOf()),
                    AstFunctionDeclaration("i", listOf(Int32), Y, listOf()),
                    AstFunctionDeclaration("j", listOf(Int32), Int32, listOf())
            ),
            listOf(
                    AstFunctionDefinition("add", listOf("x", "y"),
                            listOf(
                                    VariableAssignmentStatement("z",
                                            FunctionCallExpression("internal_add_i32",
                                                    listOf(
                                                            IdentifierExpression("x"),
                                                            IdentifierExpression("y")
                                                    )
                                            )
                                    ),
                                    VariableAssignmentStatement("w",
                                            FunctionCallExpression("internal_add_i32",
                                                    listOf(
                                                            FunctionCallExpression("internal_add_i32",
                                                                    listOf(
                                                                            IdentifierExpression("x"),
                                                                            IdentifierExpression("z")
                                                                    )
                                                            ),
                                                            IdentifierExpression("z")
                                                    )
                                            )
                                    ),
                                    ReturnStatement(IdentifierExpression("w"))
                            )
                    ),
                    AstFunctionDefinition("f", listOf("x"),
                            listOf(
                                    VariableAssignmentStatement("z",
                                            FieldGetterExpression("x", "a")
                                    ),
                                    ReturnStatement(IdentifierExpression("z"))
                            )
                    ),
                    AstFunctionDefinition("g", listOf("a"),
                            listOf(
                                    VariableAssignmentStatement("z",
                                            AllocateStructExpression("X",
                                                    listOf(
                                                            IdentifierExpression("a")
                                                    )
                                            )
                                    ),
                                    ReturnStatement(IdentifierExpression("z"))
                            )
                    ),
                    AstFunctionDefinition("h", listOf("a"),
                            listOf(
                                    VariableAssignmentStatement("x",
                                            AllocateStructExpression("X",
                                                    listOf(
                                                            IdentifierExpression("a")
                                                    )
                                            )
                                    ),
                                    VariableAssignmentStatement("r",
                                            FieldGetterExpression("x", "a")
                                    ),
                                    ReturnStatement(IdentifierExpression("r"))
                            )
                    ),
                    AstFunctionDefinition("i", listOf("a"),
                            listOf(
                                    VariableAssignmentStatement("x",
                                            AllocateStructExpression("X",
                                                    listOf(
                                                            IdentifierExpression("a")
                                                    )
                                            )
                                    ),
                                    VariableAssignmentStatement("y",
                                            AllocateStructExpression("Y",
                                                    listOf(
                                                            IdentifierExpression("x")
                                                    )
                                            )
                                    ),
                                    ReturnStatement(IdentifierExpression("y"))
                            )
                    ),
                    AstFunctionDefinition("j", listOf("a"),
                            listOf(
                                    VariableAssignmentStatement("x",
                                            AllocateStructExpression("X",
                                                    listOf(
                                                            IdentifierExpression("a")
                                                    )
                                            )
                                    ),
                                    VariableAssignmentStatement("y",
                                            AllocateStructExpression("Y",
                                                    listOf(
                                                            IdentifierExpression("x")
                                                    )
                                            )
                                    ),
                                    VariableAssignmentStatement("w", FieldGetterExpression("y", "x")),
                                    VariableAssignmentStatement("b", FieldGetterExpression("w", "a")),
                                    ReturnStatement(IdentifierExpression("b"))
                            )
                    )
            ),
            listOf(X, Y),
            listOf()
    )

    /*
    var ir = convert(program)
    println(ir)
    val passes: List<(IrModule) -> IrModule> = listOf({ i -> HeapToStackPass(i).optimize() })
    passes.forEach { ir = it(ir) }
    println(ir)

    val file = File.createTempFile("llvm", "out.ll")
    val backend = LLVMBackend(ir)
    backend.output(file)
    println(file.readText())
    */

    val code = """
struct X {
  a : Int
}
struct Y {
  x : X
}

internal_add_i32 :: Int32 -> Int32 -> Int32
add :: Int32 -> Int32 -> Int32
add x y = internal_add_i32(x, y).

f :: X -> Int32
f x = x.a.

g :: Int32 -> X
g a = X{a}.
"""
    val module: AstModule = compileString("parser_test", code)
    println(module)

}

fun compileString(moduleName: String, code: String): AstModule {
    val stream = ANTLRInputStream(code)
    val lexer = LangLexer(stream)
    val tokens = CommonTokenStream(lexer)
    val parser = LangParser(tokens)
    val result = parser.module()
    val astBuilder = ASTBuilder(moduleName, code)
    return astBuilder.visitModule(result)
}
