package com.ziur.phone.core

/** Fail-open placeholder pending a verified number-prefix index parser. */
class GeoDatabase : LocalRegionLookup {
    override fun lookup(number: String): String? = null
}
