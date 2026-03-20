package com.padelaragon.app

import android.app.Application
import com.padelaragon.app.data.favorites.FavoritesManager

class PadelAragonApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FavoritesManager.init(this)
    }
}
