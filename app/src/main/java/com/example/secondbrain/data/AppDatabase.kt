package com.example.secondbrain.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.example.secondbrain.data.dao.TextItemDao
import com.example.secondbrain.data.dao.ImageItemDao
import com.example.secondbrain.data.dao.LinkItemDao
import com.example.secondbrain.data.entities.TextItemEntity
import com.example.secondbrain.data.entities.ImageItemEntity
import com.example.secondbrain.data.entities.LinkItemEntity

@Database(
    entities = [
        TextItemEntity::class,
        ImageItemEntity::class,
        LinkItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun textItemDao(): TextItemDao
    abstract fun imageItemDao(): ImageItemDao
    abstract fun linkItemDao(): LinkItemDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "second_brain_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
} 