package com.ziur.phone.core

import android.icu.text.Transliterator
import java.text.Normalizer
import java.util.Locale

object T9Engine {
    private val hanLatin by lazy { Transliterator.getInstance("Han-Latin; Latin-ASCII") }
    private val keyMap = mapOf(
        'a' to '2', 'b' to '2', 'c' to '2',
        'd' to '3', 'e' to '3', 'f' to '3',
        'g' to '4', 'h' to '4', 'i' to '4',
        'j' to '5', 'k' to '5', 'l' to '5',
        'm' to '6', 'n' to '6', 'o' to '6',
        'p' to '7', 'q' to '7', 'r' to '7', 's' to '7',
        't' to '8', 'u' to '8', 'v' to '8',
        'w' to '9', 'x' to '9', 'y' to '9', 'z' to '9'
    )
    fun digitsForName(name: String): String {
        val latin = hanLatin.transliterate(name.lowercase(Locale.ROOT))
        val folded = Normalizer.normalize(latin, Normalizer.Form.NFD)
            .filterNot { Character.getType(it) == Character.NON_SPACING_MARK.toInt() }
        return buildString { folded.forEach { c -> keyMap[c]?.let(::append) } }
    }
    fun matches(name: String, query: String): Boolean {
        val digits = query.filter(Char::isDigit)
        return digits.isNotEmpty() && digitsForName(name).contains(digits)
    }
}
