package com.example.secondbrain.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.example.secondbrain.data.dao.ChatMessageDao
import com.example.secondbrain.data.dao.ConversationDao
import com.example.secondbrain.data.dao.ImageItemDao
import com.example.secondbrain.data.dao.LinkItemDao
import com.example.secondbrain.data.dao.TagDao
import com.example.secondbrain.data.dao.TextItemDao
import com.example.secondbrain.data.entities.ChatMessageEntity
import com.example.secondbrain.data.entities.ConversationEntity
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
        LinkItemTagCrossRef::class,
        ChatMessageEntity::class,
        ConversationEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun textItemDao(): TextItemDao
    abstract fun imageItemDao(): ImageItemDao
    abstract fun linkItemDao(): LinkItemDao
    abstract fun tagDao(): TagDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun conversationDao(): ConversationDao

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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE text_items ADD COLUMN embedding TEXT")
                database.execSQL("ALTER TABLE image_items ADD COLUMN embedding TEXT")
                database.execSQL("ALTER TABLE link_items ADD COLUMN embedding TEXT")
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS ChatMessageEntity (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        role TEXT NOT NULL,
                        content TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        sourceIds TEXT,
                        sourceTypes TEXT
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create ConversationEntity table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS ConversationEntity (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())

                // Add conversationId column to existing chat messages (default 0)
                database.execSQL(
                    "ALTER TABLE ChatMessageEntity ADD COLUMN conversationId INTEGER NOT NULL DEFAULT 0"
                )

                // If there are existing messages, migrate them to a "Previous Conversation"
                database.execSQL(
                    "INSERT INTO ConversationEntity (title, createdAt) SELECT 'Previous Conversation', 0 WHERE (SELECT COUNT(*) FROM ChatMessageEntity) > 0"
                )
                database.execSQL(
                    "UPDATE ChatMessageEntity SET conversationId = (SELECT COALESCE(MAX(id), 0) FROM ConversationEntity) WHERE conversationId = 0"
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
