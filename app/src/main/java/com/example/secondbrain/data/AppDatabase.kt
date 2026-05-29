package com.example.secondbrain.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.example.secondbrain.data.dao.ImageItemDao
import com.example.secondbrain.data.dao.LinkItemDao
import com.example.secondbrain.data.dao.TagDao
import com.example.secondbrain.data.dao.TextItemDao
import com.example.secondbrain.data.entities.ImageItemEntity
import com.example.secondbrain.data.entities.ImageItemTagCrossRef
import com.example.secondbrain.data.entities.LinkItemEntity
import com.example.secondbrain.data.entities.LinkItemTagCrossRef
import com.example.secondbrain.data.entities.TagEntity
import com.example.secondbrain.data.entities.TextItemEntity
import com.example.secondbrain.data.entities.TextItemTagCrossRef

@Database(
    entities = [
        TextItemEntity::class,
        ImageItemEntity::class,
        LinkItemEntity::class,
        TagEntity::class,
        TextItemTagCrossRef::class,
        ImageItemTagCrossRef::class,
        LinkItemTagCrossRef::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun textItemDao(): TextItemDao
    abstract fun imageItemDao(): ImageItemDao
    abstract fun linkItemDao(): LinkItemDao
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS TagEntity (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL UNIQUE)"
                )
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS TextItemTagCrossRef (textItemId INTEGER NOT NULL, tagId INTEGER NOT NULL, PRIMARY KEY (textItemId, tagId))"
                )
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS ImageItemTagCrossRef (imageItemId INTEGER NOT NULL, tagId INTEGER NOT NULL, PRIMARY KEY (imageItemId, tagId))"
                )
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS LinkItemTagCrossRef (linkItemId INTEGER NOT NULL, tagId INTEGER NOT NULL, PRIMARY KEY (linkItemId, tagId))"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "second_brain_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
