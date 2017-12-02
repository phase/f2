package f2

import f2.ast.*
import f2.ir.convert

fun main(args: Array<String>) {
    val int = Type("Int")
    val X = AstStruct("X", listOf(AstField("a", int)), listOf(), listOf(), listOf())
    /*
    struct X {
      let x : Int
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
     */
    val program: AstModule = AstModule(
            "addition",
            listOf(
                    AstFunctionDeclaration("internal_add_i32", listOf(int, int), int),
                    AstFunctionDeclaration("add", listOf(int, int), int),
                    AstFunctionDeclaration("f", listOf(X), int)
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
                    )
            ),
            listOf(X),
            listOf()
    )

    val ir = convert(program)
    println(ir)
}
