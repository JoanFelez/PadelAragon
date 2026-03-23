package com.padelaragon.app

import android.app.Application
import com.padelaragon.app.data.favorites.FavoritesManager
import com.padelaragon.app.data.local.AppDatabase
import com.padelaragon.app.data.repository.LeagueRepository

class PadelAragonApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FavoritesManager.init(this)
        val db = AppDatabase.getInstance(this)
        LeagueRepository.init(db)
    }
}
