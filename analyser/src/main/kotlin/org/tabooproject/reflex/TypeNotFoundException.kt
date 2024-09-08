package org.tabooproject.reflex

class TypeNotFoundException(val typeName: String) : Exception("Type not found: $typeName")
