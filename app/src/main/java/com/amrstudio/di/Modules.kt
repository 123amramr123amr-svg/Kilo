package com.amrstudio.di

import android.content.Context
import com.amrstudio.AmrStudioApplication
import com.amrstudio.data.database.AmrStudioDatabase
import com.amrstudio.data.preferences.PreferenceManager
import com.amrstudio.data.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@AmrStudioApplication context: Context): AmrStudioDatabase {
        return AmrStudioDatabase.getInstance(context)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object PreferenceModule {
    @Provides
    @Singleton
    fun providePreferenceManager(@AmrStudioApplication context: Context): PreferenceManager {
        return PreferenceManager.create(context)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideUserRepository(database: AmrStudioDatabase): UserRepository = UserRepository(database)

    @Provides
    @Singleton
    fun provideApiKeyRepository(database: AmrStudioDatabase): ApiKeyRepository = ApiKeyRepository(database)

    @Provides
    @Singleton
    fun provideModelRepository(database: AmrStudioDatabase): ModelRepository = ModelRepository(database)

    @Provides
    @Singleton
    fun provideChatRepository(database: AmrStudioDatabase): ChatRepository = ChatRepository(database)

    @Provides
    @Singleton
    fun provideMessageRepository(database: AmrStudioDatabase): MessageRepository = MessageRepository(database)

    @Provides
    @Singleton
    fun provideProjectRepository(database: AmrStudioDatabase): ProjectRepository = ProjectRepository(database)

    @Provides
    @Singleton
    fun provideProjectFileRepository(database: AmrStudioDatabase): ProjectFileRepository = ProjectFileRepository(database)

    @Provides
    @Singleton
    fun provideProjectVersionRepository(database: AmrStudioDatabase): ProjectVersionRepository = ProjectVersionRepository(database)

    @Provides
    @Singleton
    fun provideConnectorRepository(database: AmrStudioDatabase): ConnectorRepository = ConnectorRepository(database)

    @Provides
    @Singleton
    fun provideAgentTaskRepository(database: AmrStudioDatabase): AgentTaskRepository = AgentTaskRepository(database)

    @Provides
    @Singleton
    fun provideDownloadRepository(database: AmrStudioDatabase): DownloadRepository = DownloadRepository(database)

    @Provides
    @Singleton
    fun provideNotificationRepository(database: AmrStudioDatabase): NotificationRepository = NotificationRepository(database)

    @Provides
    @Singleton
    fun providePromptRepository(database: AmrStudioDatabase): PromptRepository = PromptRepository(database)

    @Provides
    @Singleton
    fun provideSettingRepository(database: AmrStudioDatabase): SettingRepository = SettingRepository(database)
}