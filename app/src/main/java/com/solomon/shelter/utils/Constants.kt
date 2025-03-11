package com.solomon.shelter.utils

object Constants {
    // Network constants
    const val DEFAULT_PORT = 8080
    const val SERVICE_TYPE = "_bunker._tcp."
    const val SERVICE_NAME = "Bunker Game"

    // Game constants
    const val MIN_PLAYERS = 4
    const val MAX_PLAYERS = 18
    const val MIN_DISCUSSION_TIME = 30
    const val MAX_DISCUSSION_TIME = 180
    const val MIN_VOTE_TIME = 30
    const val MAX_VOTE_TIME = 120

    // Database constants
    const val DATABASE_NAME = "bunker_database"

    // Characteristic IDs
    const val PROFESSION_ID = 1L
    const val HEALTH_ID = 2L
    const val PSYCHOLOGY_ID = 3L
    const val HOBBY_ID = 4L
    const val ADDITIONAL_INFO_ID = 5L
    const val BAGGAGE_ID = 6L
    const val ACTION_CARD_ID = 0L

    // Card utility index ranges
    const val MIN_UTILITY_INDEX = 1
    const val MAX_UTILITY_INDEX = 10

    // Shared preferences
    const val PREFS_NAME = "bunker_prefs"
    const val PREF_PLAYER_NAME = "player_name"
    const val PREF_USER_ID = "user_id"
}