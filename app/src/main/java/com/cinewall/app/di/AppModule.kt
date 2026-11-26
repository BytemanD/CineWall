package com.cinewall.app.di

import android.content.Context
import com.cinewall.app.repository.ConfigRepository
import com.cinewall.app.repository.LocalMediaRepository
import com.cinewall.app.repository.MediaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** 简单的单例 DI 持有器（Application 生命周期） */
object AppModule {
    @Volatile
    private var _repository: MediaRepository? = null
    @Volatile
    private var _appScope: AppScopeHolder? = null

    fun init(context: Context) {
        val scope = AppScopeHolder(context.applicationContext)
        _appScope = scope
        _repository = LocalMediaRepository(context.applicationContext, scope.scope)
    }

    val isInitialized: Boolean get() = _repository != null

    val repository: MediaRepository
        get() = _repository ?: error("AppModule not initialized. Call AppModule.init(context) in Application.onCreate or MainActivity.")

    val scope: CoroutineScope
        get() = _appScope?.scope ?: error("AppModule not initialized.")
}

class AppScopeHolder(context: Context) {
    val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
}
