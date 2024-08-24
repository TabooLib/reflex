package org.tabooproject.reflex

class EnumNotFoundException(val enumName: String) : Exception("Enum not found: $enumName")
