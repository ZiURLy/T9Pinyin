package com.ziur.phone.telecom

import android.telecom.Call
import android.telecom.InCallService

class ZiURInCallService : InCallService() {
    override fun onCallAdded(call: Call) { super.onCallAdded(call) }
}
