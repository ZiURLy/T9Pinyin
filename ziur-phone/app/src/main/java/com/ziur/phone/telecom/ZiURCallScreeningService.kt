package com.ziur.phone.telecom

import android.telecom.Call
import android.telecom.CallScreeningService
import com.ziur.phone.core.*

class ZiURCallScreeningService : CallScreeningService() {
    override fun onScreenCall(details: Call.Details) {
        val number = details.handle?.schemeSpecificPart
        val prefs = getSharedPreferences("ziur_call_rules", MODE_PRIVATE)
        val config = BlockingConfig(
            enabled = prefs.getBoolean("blocking_enabled", false),
            blockUnknownRegion = prefs.getBoolean("block_unknown_region", false)
        )
        val decision = if (details.callDirection == Call.Details.DIRECTION_INCOMING) {
            runCatching {
                CallPolicy(GeoDatabase()).decide(
                    CallContext(number, isContact = false, wasDialedByUser = false), config
                )
            }.getOrDefault(Decision.ALLOW)
        } else Decision.ALLOW
        val block = decision == Decision.BLOCK
        val response = CallResponse.Builder()
            .setDisallowCall(block).setRejectCall(block)
            .setSkipCallLog(false).setSkipNotification(false).build()
        respondToCall(details, response)
    }
}
