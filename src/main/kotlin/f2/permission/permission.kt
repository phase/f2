package f2.permission

data class Permission(val name: String, val propagates: Boolean) {
    override fun toString(): String = name
}

val UndefinedPermission = Permission("+_", false)
val ExternalPermission = Permission("+External", false)
val MutablePermission = Permission("+Mutable", false)

val permissions = listOf(
        ExternalPermission,
        MutablePermission
)
