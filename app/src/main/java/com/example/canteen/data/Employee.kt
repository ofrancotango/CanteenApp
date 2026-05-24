package com.example.canteen.data

data class Employee(
    val name: String, // Keep for backward compatibility/display
    val company: String,
    val firstName: String? = null,
    val lastName: String? = null
)
