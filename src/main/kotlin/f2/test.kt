package f2

import f2.ast.*
import f2.backend.llvm.LLVMBackend
import f2.ir.IrModule
import f2.ir.convert
import f2.ir.optimize.pass.HeapToStackPass
import f2.type.Int32
import java.io.File

fun main(args: Array<String>) {
    val X = AstStruct("X", listOf(AstField("a", Int32)), listOf(), listOf(), listOf())
    /*
    struct X {
      let a : Int
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
     */
    val program: AstModule = AstModule(
            "addition",
            listOf(
                    AstFunctionDeclaration("internal_add_i32", listOf(Int32, Int32), Int32),
                    AstFunctionDeclaration("add", listOf(Int32, Int32), Int32),
                    AstFunctionDeclaration("f", listOf(X), Int32),
                    AstFunctionDeclaration("g", listOf(Int32), X),
                    AstFunctionDeclaration("h", listOf(Int32), Int32)
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
                    )
            ),
            listOf(X),
            listOf()
    )

    var ir = convert(program)
    println(ir)
    val passes: List<(IrModule) -> IrModule> = listOf({ i -> HeapToStackPass(i).optimize() })
    passes.forEach { ir = it(ir) }
    println(ir)

    val file = File.createTempFile("llvm", "out.ll")
    val backend = LLVMBackend(ir)
    backend.output(file)
    println(file.readText())
}
