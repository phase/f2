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
        z.
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
                                    ReturnStatement(IdentifierExpression("z"))
                            )
                    )
            ),
            listOf(),
            listOf()
    )

    val ir = convert(program)
    println(ir)
}
