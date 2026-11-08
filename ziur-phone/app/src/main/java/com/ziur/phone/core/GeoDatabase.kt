package com.ziur.phone.core

/**
 * Deliberate fail-open placeholder.
 * The source APK's geographic database label table was recovered, but its
 * number-prefix/index section is not yet fully decoded or validated.
 * Do not block calls based on an incomplete mapping.
 */
class GeoDatabase : LocalRegionLookup {
    override fun lookup(number: String): String? = null
}
