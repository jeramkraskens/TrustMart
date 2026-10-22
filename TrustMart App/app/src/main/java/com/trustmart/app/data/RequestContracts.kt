package com.trustmart.app.data

/** Django requires confirmation in addition to the new password. */
fun passwordChangeFields(old: String, new: String): Map<String, String> = mapOf(
    "old_password" to old, "new_password" to new, "password_confirm" to new,
)
