package f2.ast

import f2.parser.LangBaseVisitor
import f2.parser.LangParser
import f2.permission.Permission
import f2.permission.UndefinedPermission
import f2.permission.permissions
import f2.type.Type
import f2.type.UndefinedType
import f2.type.primitives
import org.antlr.v4.runtime.tree.TerminalNode

class ASTBuilder(val moduleName: String, val source: String) : LangBaseVisitor<Any>() {

    val structs: MutableList<AstStruct> = mutableListOf()

    fun getType(name: String): Type {
        primitives.forEach {
            if (it.name == name) return it
        }
        structs.forEach {
            if (it.name == name) return it
        }

        return UndefinedType
    }

    fun getPermission(name: String): Permission {
        permissions.forEach {
            if (it.name == name) return it
        }
        return UndefinedPermission
    }

    fun LangParser.TypeContext.toType(): Type = getType(text)
    fun List<LangParser.TypeContext>.toType(): List<Type> = map { getType(it.text) }
    fun TerminalNode.string(): String = this.symbol.text

    override fun visitModule(ctx: LangParser.ModuleContext): AstModule {
        val externalDeclarations = ctx.externalDeclaration()

        externalDeclarations.mapNotNull { it.structDeclaration() }.forEach { structs.add(visitStructDeclaration(it)) }
        val functionDeclarations = externalDeclarations.mapNotNull { it.functionDeclaration() }.map { visitFunctionDeclaration(it) }
        val functionDefinitions = externalDeclarations.mapNotNull { it.functionDefinition() }.map { visitFunctionDefinition(it) }

        return AstModule(moduleName, functionDeclarations, functionDefinitions, structs, listOf(), source, mutableListOf())
    }

    override fun visitFunctionDeclaration(ctx: LangParser.FunctionDeclarationContext): AstFunctionDeclaration {
        val name = ctx.ID().string()
        val types = ctx.type().toType()
        val permissions = ctx.PERMISSION().map { it.string() }.map { getPermission(it) }

        val returnType = types.last()
        val argumentTypes = types.subList(0, types.size - 1)

        return AstFunctionDeclaration(name, argumentTypes, returnType, permissions, ctx.debugInfo())
    }

    override fun visitFunctionDefinition(ctx: LangParser.FunctionDefinitionContext): AstFunctionDefinition {
        val names = ctx.ID().map { it.string() }
        val functionName = names.first()
        val arguments = names.subList(1, names.size)
        val statements = ctx.statement().map { visitStatement(it) }.toMutableList()

        // convert the final expression to statements
        val expression = visitExpression(ctx.expression())
        if (expression is IdentifierExpression) {
            statements.add(ReturnStatement(expression.debugInfo, expression))
        } else {
            statements.add(VariableAssignmentStatement(expression.debugInfo, "__return_expression", expression))
            statements.add(ReturnStatement(expression.debugInfo, IdentifierExpression(ctx.debugInfo(), "__return_expression")))
        }

        return AstFunctionDefinition(functionName, arguments, statements, ctx.debugInfo())
    }

    override fun visitStructDeclaration(ctx: LangParser.StructDeclarationContext): AstStruct {
        val name = ctx.ID().string()
        val fields = ctx.field().map { visitField(it) }
        return AstStruct(name, fields, listOf(), listOf(), listOf(), ctx.debugInfo())
    }

    override fun visitField(ctx: LangParser.FieldContext): AstField {
        val name = ctx.ID().string()
        val type = ctx.type().toType()
        return AstField(name, type, ctx.debugInfo())
    }

    override fun visitStatement(ctx: LangParser.StatementContext): Statement {
        ctx.fieldSetter()?.let {
            val structName = it.ID(0).string()
            val fieldName = it.ID(1).string()
            val expression = visitExpression(it.expression())
            return FieldSetterStatement(ctx.debugInfo(), structName, fieldName, expression)
        }
        ctx.returnStatement()?.let {
            val expression = visitExpression(it.expression())
            return ReturnStatement(ctx.debugInfo(), expression)
        }
        ctx.variableAssignment()?.let {
            val name = it.ID().string()
            val expression = visitExpression(it.expression())
            return VariableAssignmentStatement(ctx.debugInfo(), name, expression)
        }
        throw IllegalStateException("$ctx not a statement?")
    }

    override fun visitExpression(ctx: LangParser.ExpressionContext): Expression {
        ctx.ID()?.string()?.let {
            return IdentifierExpression(ctx.debugInfo(), it)
        }
        ctx.allocateStruct()?.let {
            val name = it.type().ID().string()
            val arguments = it.arguments().expression().map { visitExpression(it) }
            return AllocateStructExpression(ctx.debugInfo(), name, arguments)
        }
        ctx.fieldGetter()?.let {
            val structName = it.ID(0).string()
            val fieldName = it.ID(1).string()
            return FieldGetterExpression(ctx.debugInfo(), structName, fieldName)
        }
        ctx.functionCall()?.let {
            val functionName = it.ID().string()
            val arguments = it.arguments().expression().map { visitExpression(it) }
            return FunctionCallExpression(ctx.debugInfo(), functionName, arguments)
        }
        throw IllegalStateException("$ctx not an expression?")
    }

}
