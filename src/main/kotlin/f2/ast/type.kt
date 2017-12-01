package f2.ast

open class Type(val name: String) {
    override fun toString(): String = name
}

val UndefinedType = Type("Undefined")
