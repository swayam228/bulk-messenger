package com.example.bulkmessenger.util

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract

data class PickedContact(val name: String?, val phoneNumber: String)

/**
 * Resolves a contact URI returned by ACTION_PICK against Phone.CONTENT_TYPE into a
 * name + normalized number. Keeping this out of the Activity keeps the picker logic testable.
 */
object ContactPickerHelper {
    fun resolve(context: Context, contactUri: Uri): PickedContact? {
        val cursor: Cursor = context.contentResolver.query(
            contactUri,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            ),
            null, null, null
        ) ?: return null

        cursor.use {
            if (it.moveToFirst()) {
                val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val number = if (numberIdx >= 0) it.getString(numberIdx) else null
                val name = if (nameIdx >= 0) it.getString(nameIdx) else null
                if (number != null) return PickedContact(name, number)
            }
        }
        return null
    }
}
