package com.solomon.shelter.data.models

data class GameInfo(
    val id: Long,
    val name: String,
    val host: String,
    val currentPlayers: Int,
    val maxPlayers: Int,
    val hostAddress: String,
    val port: Int
)