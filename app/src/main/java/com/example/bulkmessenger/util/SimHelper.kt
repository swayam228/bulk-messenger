package com.example.bulkmessenger.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat

data class SimOption(val subscriptionId: Int, val slotIndex: Int, val label: String)

/**
 * Wraps SubscriptionManager defensively — some OEMs behave inconsistently here, so any failure
 * (missing permission, no telephony service, SecurityException) just means "no picker shown"
 * rather than a crash.
 */
object SimHelper {
    fun getActiveSims(context: Context): List<SimOption> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return emptyList()
        }
        return try {
            val manager = context.getSystemService(SubscriptionManager::class.java) ?: return emptyList()
            val infos = manager.activeSubscriptionInfoList ?: return emptyList()
            infos.map { info ->
                val carrierName = info.carrierName?.toString()?.takeIf { it.isNotBlank() }
                    ?: info.displayName?.toString()?.takeIf { it.isNotBlank() }
                    ?: "Unknown carrier"
                // Two SIMs on the same network report the same carrier name (e.g. both "BSNL
                // Mobile"), so the slot number is always prefixed — otherwise there's no way to
                // tell them apart in the picker.
                SimOption(
                    subscriptionId = info.subscriptionId,
                    slotIndex = info.simSlotIndex,
                    label = "SIM ${info.simSlotIndex + 1} – $carrierName"
                )
            }
        } catch (e: SecurityException) {
            emptyList()
        }
    }
}
