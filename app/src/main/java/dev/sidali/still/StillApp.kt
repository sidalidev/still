package dev.sidali.still

import android.app.Application
import android.os.StrictMode

/** Debug builds only: StrictMode logs any disk or network access on the main thread, and leaks. */
class StillApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build())
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build())
        }
    }
}
