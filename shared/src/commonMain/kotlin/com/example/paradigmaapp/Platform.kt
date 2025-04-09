package com.example.paradigmaapp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform