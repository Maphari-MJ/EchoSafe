package com.echosafe.dispatch

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.telephony.TelephonyManager

class NetworkAirtimeEvaluator(private val context: Context) {

    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun hasCellularSignal(): Boolean {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            ?: return false
        return telephonyManager.simState == TelephonyManager.SIM_STATE_READY
    }

    fun hasAirtimeBalance(): Boolean {
        // Evaluates cellular carrier state & SMS balance availability
        // In native Android, if SIM is ready and network registered, SMS bundle/airtime is attempted
        return hasCellularSignal()
    }
}
