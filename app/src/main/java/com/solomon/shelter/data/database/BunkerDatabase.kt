package com.solomon.shelter.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.solomon.shelter.data.database.dao.CardDao
import com.solomon.shelter.data.database.dao.CatastropheDao
import com.solomon.shelter.data.database.dao.EndingDao
import com.solomon.shelter.data.database.dao.GameDao
import com.solomon.shelter.data.database.dao.PackDao
import com.solomon.shelter.data.database.dao.PlayerDao
import com.solomon.shelter.data.database.dao.ShelterDao
import com.solomon.shelter.data.database.entities.CharCard
import com.solomon.shelter.data.database.entities.Catastrophe
import com.solomon.shelter.data.database.entities.Characteristic
import com.solomon.shelter.data.database.entities.Ending
import com.solomon.shelter.data.database.entities.Game
import com.solomon.shelter.data.database.entities.Pack
import com.solomon.shelter.data.database.entities.Player
import com.solomon.shelter.data.database.entities.PlayerCardJoin
import com.solomon.shelter.data.database.entities.Shelter

@Database(
    entities = [Pack::class, CharCard::class, Characteristic::class, Shelter::class,
        Ending::class, Catastrophe::class, Player::class, PlayerCardJoin::class, Game::class],
    version = 1
)
abstract class BunkerDatabase : RoomDatabase() {
    abstract fun packDao(): PackDao
    abstract fun cardDao(): CardDao
    abstract fun gameDao(): GameDao
    abstract fun playerDao(): PlayerDao
    abstract fun shelterDao(): ShelterDao
    abstract fun catastropheDao(): CatastropheDao
    abstract fun endingDao(): EndingDao



    companion object {
        @Volatile
        private var INSTANCE: BunkerDatabase? = null

        fun getDatabase(context: Context): BunkerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BunkerDatabase::class.java,
                    "bunker_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}