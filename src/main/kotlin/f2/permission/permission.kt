package f2.permission

data class Permission(val name: String, val propagates: Boolean)

val Mutable = Permission("Mutable", false)
