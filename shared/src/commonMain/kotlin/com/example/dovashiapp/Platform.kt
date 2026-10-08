package com.example.dovashiapp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform