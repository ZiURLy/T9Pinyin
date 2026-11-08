package com.ziur.phone.telecom

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import com.ziur.phone.core.BlockingConfig
import com.ziur.phone.core.CallContext
import com.ziur.phone.core.CallPolicy
import com.ziur.phone.core.Decision
import com.ziur.phone.core.GeoDatabase

class ZiURCallScreeningService : CallScreeningService() {
    override fun onScreenCall(details: Call.Details) {
        val number = details.handle?.schemeSpecificPart
        val prefs = getSharedPreferences("ziur_call_rules", MODE_PRIVATE)
        val config = BlockingConfig(
            enabled = prefs.getBoolean("blocking_enabled", false),
            blockUnknownRegion = false
        )
        val incoming = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            details.callDirection == Call.Details.DIRECTION_INCOMING
        val decision = if (incoming) {
            runCatching {
                CallPolicy(GeoDatabase()).decide(
                    CallContext(number, isContact = false, wasDialedByUser = false),
                    config
                )
            }.getOrDefault(Decision.ALLOW)
        } else Decision.ALLOW

        val block = decision == Decision.BLOCK
        val response = CallResponse.Builder()
            .setDisallowCall(block)
            .setRejectCall(block)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()
        respondToCall(details, response)
    }
}
