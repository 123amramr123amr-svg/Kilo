package com.personalaiagent.di

import android.content.Context
import androidx.room.Room
import com.personalaiagent.core.AppConstants
import com.personalaiagent.data.local.AppDatabase
import com.personalaiagent.data.local.ConversationDao
import com.personalaiagent.data.local.MessageDao
import com.personalaiagent.data.local.ModelDao
import com.personalaiagent.data.local.ProviderDao
import com.personalaiagent.data.local.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppConstants.DATABASE_NAME)
            .build()

    @Provides
    fun provideProviderDao(database: AppDatabase): ProviderDao = database.providerDao()

    @Provides
    fun provideModelDao(database: AppDatabase): ModelDao = database.modelDao()

    @Provides
    fun provideConversationDao(database: AppDatabase): ConversationDao = database.conversationDao()

    @Provides
    fun provideMessageDao(database: AppDatabase): MessageDao = database.messageDao()

    @Provides
    fun provideTaskDao(database: AppDatabase): TaskDao = database.taskDao()
}
