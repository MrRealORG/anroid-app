package com.fbr.ntn

import android.app.Application
import com.fbr.ntn.data.AppContainer

class FbrNtnApplication : Application() {
    val container by lazy { AppContainer(this) }
}
