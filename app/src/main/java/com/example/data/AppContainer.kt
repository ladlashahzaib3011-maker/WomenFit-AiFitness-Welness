package com.example.data

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.security.SessionDataStore

/**
 * Application-scoped dependency container ensuring a single source of truth
 * for SessionDataStore, AuthRepository, and FitnessRepository across ViewModels.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(appContext)
    }

    val sessionDataStore: SessionDataStore by lazy {
        SessionDataStore(appContext)
    }

    val authRepository: AuthRepository by lazy {
        SupabaseAuthRepository(
            sessionDataStore = sessionDataStore,
            userDao = database.userDao()
        )
    }

    val fitnessRepository: FitnessRepository by lazy {
        FitnessRepository(
            database = database,
            activeUserIdFlow = authRepository.activeUserId,
            onProfileSync = authRepository::syncProfileUpdate
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: AppContainer? = null

        fun getInstance(context: Context): AppContainer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppContainer(context).also { INSTANCE = it }
            }
        }
    }
}
