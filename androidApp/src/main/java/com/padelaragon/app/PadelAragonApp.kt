package com.padelaragon.app

import android.app.Application
import android.content.pm.ApplicationInfo
import com.google.android.gms.security.ProviderInstaller
import com.padelaragon.shared.data.local.PlatformContext
import com.padelaragon.shared.data.model.League
import com.padelaragon.shared.di.AppContainer
import com.padelaragon.shared.di.MultiLeagueContainer
import com.padelaragon.shared.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PadelAragonApp : Application() {
    lateinit var multiLeagueContainer: MultiLeagueContainer
        private set

    /** The current Android UI only knows the Absoluta league until the UI is unified. */
    val container: AppContainer get() = multiLeagueContainer[League.ABSOLUTA]

    override fun onCreate() {
        super.onCreate()
        Logger.enabled = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        try {
            ProviderInstaller.installIfNeeded(this)
            Logger.d("PadelAragonApp", "Security provider updated successfully")
        } catch (e: Exception) {
            Logger.e("PadelAragonApp", "Failed to update security provider", e)
        }

        multiLeagueContainer = MultiLeagueContainer.create(PlatformContext(this))
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch { multiLeagueContainer.prewarmConnection() }
    }
}
