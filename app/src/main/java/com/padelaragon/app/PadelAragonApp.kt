package com.padelaragon.app

import android.app.Application
import android.util.Log
import com.google.android.gms.security.ProviderInstaller
import com.padelaragon.app.data.favorites.FavoritesManager
import com.padelaragon.app.data.local.AppDatabase
import com.padelaragon.app.data.network.HtmlFetcher
import com.padelaragon.app.di.AppContainer

class PadelAragonApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        try {
            ProviderInstaller.installIfNeeded(this)
            Log.d("PadelAragonApp", "Security provider updated successfully")
        } catch (e: Exception) {
            Log.e("PadelAragonApp", "Failed to update security provider", e)
        }

        // Prewarm HTTPS connection during startup (saves TLS handshake on first real request)
        HtmlFetcher.prewarmConnection("https://padelfederacion.es/pAGINAS/ARAPADEL/Ligas_Calendario.asp")

        FavoritesManager.init(this)
        val db = AppDatabase.getInstance(this)
        container = AppContainer(db, cacheDir)
    }
}
