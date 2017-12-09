package f2.permission

data class Permission(val name: String, val propagates: Boolean)

val UndefinedPermission = Permission("_", false)
val MutablePermission = Permission("Mutable", false)

val permissions = listOf(MutablePermission)
