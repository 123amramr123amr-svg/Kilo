package com.personalaiagent.di

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.personalaiagent.BuildConfig
import com.personalaiagent.core.AppConstants
import com.personalaiagent.core.DefaultIdGenerator
import com.personalaiagent.core.IdGenerator
import com.personalaiagent.core.SystemTimeProvider
import com.personalaiagent.core.TimeProvider
import com.personalaiagent.data.backup.BackupManager
import com.personalaiagent.data.prefs.SettingsRepositoryImpl
import com.personalaiagent.data.remote.AnthropicClient
import com.personalaiagent.data.remote.HttpEngine
import com.personalaiagent.data.remote.OpenAiCompatibleClient
import com.personalaiagent.data.remote.ProviderClientRouter
import com.personalaiagent.data.secure.AndroidKeystoreCipher
import com.personalaiagent.data.secure.EncryptedCredentialStore
import com.personalaiagent.data.secure.KeyValueStore
import com.personalaiagent.data.secure.SharedPreferencesKeyValueStore
import com.personalaiagent.data.secure.SecretCipher
import com.personalaiagent.data.local.AppDatabase
import com.personalaiagent.domain.repository.CredentialStore
import com.personalaiagent.domain.repository.SettingsRepository
import com.personalaiagent.platform.LocaleMirror
import com.personalaiagent.platform.PrefsLocaleMirror
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver =
        context.contentResolver

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        isLenient = true
    }

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    fun provideTimeProvider(): TimeProvider = SystemTimeProvider

    @Provides
    @Singleton
    fun provideIdGenerator(): IdGenerator = DefaultIdGenerator

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(AppConstants.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(AppConstants.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(AppConstants.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)

        if (BuildConfig.DEBUG) {
            // BASIC never logs bodies, and credential headers are redacted.
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
                redactHeader("Authorization")
                redactHeader("x-api-key")
            }
            builder.addInterceptor(logging)
        }
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideHttpEngine(client: OkHttpClient): HttpEngine = HttpEngine(client)

    // Concrete return types: two unqualified ProviderClient bindings would be a
    // duplicate-binding error in Dagger.
    @Provides
    @Singleton
    fun provideOpenAiClient(http: HttpEngine, json: Json): OpenAiCompatibleClient =
        OpenAiCompatibleClient(http, json)

    @Provides
    @Singleton
    fun provideAnthropicClient(http: HttpEngine, json: Json): AnthropicClient =
        AnthropicClient(http, json)

    @Provides
    @Singleton
    fun provideRouter(
        openAi: OpenAiCompatibleClient,
        anthropic: AnthropicClient,
    ): ProviderClientRouter = ProviderClientRouter(openAi, anthropic)

    @Provides
    @Singleton
    fun provideSecretCipher(): SecretCipher = AndroidKeystoreCipher()

    @Provides
    @Singleton
    fun provideSecurePreferences(@ApplicationContext context: Context): SharedPreferences =
        context.getSharedPreferences(AppConstants.SECURE_PREFS_NAME, Context.MODE_PRIVATE)

    @Provides
    @Singleton
    fun provideKeyValueStore(preferences: SharedPreferences): KeyValueStore =
        SharedPreferencesKeyValueStore(preferences)

    @Provides
    @Singleton
    fun provideCredentialStore(
        store: KeyValueStore,
        cipher: SecretCipher,
        @IoDispatcher io: CoroutineDispatcher,
    ): CredentialStore = EncryptedCredentialStore(store, cipher, io)

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(AppConstants.DATASTORE_NAME) },
        )

    @Provides
    @Singleton
    fun provideLocaleMirror(@ApplicationContext context: Context): LocaleMirror =
        PrefsLocaleMirror(PrefsLocaleMirror.preferences(context))

    @Provides
    @Singleton
    fun provideSettingsRepository(
        dataStore: DataStore<Preferences>,
        localeMirror: LocaleMirror,
    ): SettingsRepository = SettingsRepositoryImpl(dataStore, localeMirror)

    @Provides
    @Singleton
    fun provideBackupManager(
        database: AppDatabase,
        settingsRepository: SettingsRepository,
        time: TimeProvider,
        json: Json,
    ): BackupManager = BackupManager(database, settingsRepository, time, json)
}
