package com.personalaiagent.di

import com.personalaiagent.data.repository.ChatRepositoryImpl
import com.personalaiagent.data.repository.ModelRepositoryImpl
import com.personalaiagent.data.repository.ProviderRepositoryImpl
import com.personalaiagent.data.repository.TaskRepositoryImpl
import com.personalaiagent.domain.repository.ChatRepository
import com.personalaiagent.domain.repository.ModelRepository
import com.personalaiagent.domain.repository.ProviderRepository
import com.personalaiagent.domain.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProviderRepository(impl: ProviderRepositoryImpl): ProviderRepository

    @Binds
    @Singleton
    abstract fun bindModelRepository(impl: ModelRepositoryImpl): ModelRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository
}
