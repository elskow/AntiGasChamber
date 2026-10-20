package org.unesa.agc.data

import android.content.Context

class SharedPreferencesTokenStore(context: Context) : TokenStore {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun token(): String? =
        prefs.getString(TOKEN_KEY, null)

    override suspend fun save(token: String) {
        prefs.edit().putString(TOKEN_KEY, token).apply()
    }

    override suspend fun clear() {
        prefs.edit().remove(TOKEN_KEY).apply()
    }

    private companion object {
        const val PREFS_NAME = "auth"
        const val TOKEN_KEY = "jwt"
    }
}
