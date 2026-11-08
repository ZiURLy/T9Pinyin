package com.ziur.phone.core

import android.content.Context
import android.provider.ContactsContract

data class PhoneContact(
    val contactId: Long,
    val displayName: String,
    val number: String
)

object ContactRepository {
    fun load(context: Context): List<PhoneContact> {
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val result = ArrayList<PhoneContact>()
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection, null, null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE LOCALIZED ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameCol).orEmpty()
                val number = cursor.getString(numberCol).orEmpty()
                if (name.isNotBlank() && number.isNotBlank()) {
                    result += PhoneContact(cursor.getLong(idCol), name, number)
                }
            }
        } ?: DiagnosticLog.w("CONTACT_QUERY", "ContactsContract returned null cursor")
        val contacts = result.distinctBy { "${it.contactId}:${it.number}" }
        DiagnosticLog.i("CONTACT_QUERY", "rows=" + result.size + " distinct=" + contacts.size)
        return contacts
    }
}
