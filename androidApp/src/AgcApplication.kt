package org.unesa.agc

import android.app.Application
import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.unesa.agc.push.AndroidPushBridge
import org.unesa.agc.push.AndroidPushDefaults

class AgcApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initFirebase()
        val graph = AppGraphHolder.graph
        AndroidPushDefaults.install(
            AndroidPushBridge(
                alertCenter = graph.alertCenter,
                tokenSink = { token: String ->
                    CoroutineScope(Dispatchers.Default).launch {
                        runCatching { graph.api.registerPushToken(token, "android") }
                    }
                },
            ),
        )
    }

    private fun initFirebase() {
        if (FirebaseApp.getApps(this).isNotEmpty()) return
        FirebaseApp.initializeApp(
            this as Context,
            FirebaseOptions.Builder()
                .setApiKey("AIzaSyDJtNrfMru8JJRcbNNklFjqo8a27dOmcOQ")
                .setApplicationId("1:66638218725:android:127455d7b05207d65bdb0c")
                .setProjectId("helmyl")
                .setGcmSenderId("66638218725")
                .build(),
        )
    }
}
