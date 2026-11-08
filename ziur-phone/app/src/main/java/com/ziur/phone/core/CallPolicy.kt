package com.ziur.phone.core

enum class Decision { ALLOW, BLOCK, REVIEW }
data class CallContext(
    val number: String?,
    val isContact: Boolean,
    val wasDialedByUser: Boolean,
    val isEmergency: Boolean = false
)
fun interface LocalRegionLookup { fun lookup(number: String): String? }
data class BlockingConfig(
    val enabled: Boolean = false,
    val localRegion: String = "广西柳州市",
    val blockUnknownRegion: Boolean = false,
    val allowServicePrefixes: List<String> = listOf("400", "800", "95", "96", "106"),
    val allowInternational: Boolean = false,
    val allowContacts: Boolean = true,
    val allowPreviouslyDialed: Boolean = true
)
class CallPolicy(private val regionLookup: LocalRegionLookup) {
    fun decide(context: CallContext, config: BlockingConfig): Decision {
        if (!config.enabled || context.isEmergency) return Decision.ALLOW
        if (config.allowContacts && context.isContact) return Decision.ALLOW
        if (config.allowPreviouslyDialed && context.wasDialedByUser) return Decision.ALLOW
        val raw = context.number?.trim().orEmpty()
        if (raw.isEmpty() || raw.equals("unknown", true)) {
            return if (config.blockUnknownRegion) Decision.BLOCK else Decision.REVIEW
        }
        val digits = raw.filter(Char::isDigit)
        if (config.allowServicePrefixes.any { digits.startsWith(it) }) return Decision.ALLOW
        if (raw.startsWith("+") && !raw.startsWith("+86") && config.allowInternational) return Decision.ALLOW
        val region = regionLookup.lookup(digits)
            ?: return if (config.blockUnknownRegion) Decision.BLOCK else Decision.REVIEW
        return if (region.contains(config.localRegion)) Decision.ALLOW else Decision.BLOCK
    }
}
