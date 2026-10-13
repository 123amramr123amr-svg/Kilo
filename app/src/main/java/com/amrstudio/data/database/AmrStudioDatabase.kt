package com.amrstudio.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.amrstudio.data.model.*
import com.amrstudio.data.database.Daos.*

@Database(
    entities = [
        User::class,
        ApiKey::class,
        Model::class,
        Chat::class,
        Message::class,
        Project::class,
        ProjectFile::class,
        ProjectVersion::class,
        Connector::class,
        AgentTask::class,
        Download::class,
        Notification::class,
        Prompt::class,
        Setting::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AmrStudioDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun apiKeyDao(): ApiKeyDao
    abstract fun modelDao(): ModelDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun projectDao(): ProjectDao
    abstract fun projectFileDao(): ProjectFileDao
    abstract fun projectVersionDao(): ProjectVersionDao
    abstract fun connectorDao(): ConnectorDao
    abstract fun agentTaskDao(): AgentTaskDao
    abstract fun downloadDao(): DownloadDao
    abstract fun notificationDao(): NotificationDao
    abstract fun promptDao(): PromptDao
    abstract fun settingDao(): SettingDao

    companion object {
        @Volatile
        private var INSTANCE: AmrStudioDatabase? = null

        fun getInstance(context: Context): AmrStudioDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AmrStudioDatabase::class.java,
                    "amr_studio.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}