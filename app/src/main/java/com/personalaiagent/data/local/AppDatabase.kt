package com.personalaiagent.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProviderEntity::class,
        ModelEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        TaskRunEntity::class,
        TaskStepEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun providerDao(): ProviderDao
    abstract fun modelDao(): ModelDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun taskDao(): TaskDao
}
