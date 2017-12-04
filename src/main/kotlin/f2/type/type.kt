package f2.type

open class Type(val name: String) {
    override fun toString(): String = name
}

val UndefinedType = Type("Undefined")

open class PrimitiveType(name: String) : Type(name)

val primitives = listOf(
        Int8, Int16, Int32, Int64,
        Float32, Float64
)

object Int8 : PrimitiveType("Int8")
object Int16 : PrimitiveType("Int16")
object Int32 : PrimitiveType("Int32")
object Int64 : PrimitiveType("Int32")
object Float32 : PrimitiveType("Float32")
object Float64 : PrimitiveType("Float64")
