package com.padelaragon.app

import android.app.Application
import android.util.Log
import com.google.android.gms.security.ProviderInstaller
import com.padelaragon.app.data.favorites.FavoritesManager
import com.padelaragon.app.data.local.AppDatabase
import com.padelaragon.app.data.repository.LeagueRepository

class PadelAragonApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            ProviderInstaller.installIfNeeded(this)
            Log.d("PadelAragonApp", "Security provider updated successfully")
        } catch (e: Exception) {
            Log.e("PadelAragonApp", "Failed to update security provider", e)
        }
        FavoritesManager.init(this)
        val db = AppDatabase.getInstance(this)
        LeagueRepository.init(db)
    }
}
