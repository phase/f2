package f2

import f2.ast.*
import f2.ir.convert

fun main(args: Array<String>) {
    val int = Type("Int")
    /*
    internal_add_i32 :: Int -> Int -> Int
    add :: Int -> Int -> Int
    add x y =
        let z = internal_add_i32(x, y),
        let w = internal_add_i32(internal_add_i32(x, y), z),
        w.
     */
    val program: AstModule = AstModule(
            "addition",
            listOf(
                    AstFunctionDeclaration("internal_add_i32", listOf(int, int), int),
                    AstFunctionDeclaration("add", listOf(int, int), int)
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
                                                            FunctionCallExpression("internal_add2_i32",
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
                    )
            ),
            listOf(),
            listOf()
    )

    val ir = convert(program)
    println(ir)
}
