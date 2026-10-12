package com.spendsense.app.data

import android.content.Context
import android.provider.Telephony
import kotlin.math.abs

/** Reads the SMS inbox and turns bank messages into categorized transactions. */
class SmsRepository(private val context: Context) {

    fun loadTransactions(sinceMillis: Long, overrides: Map<String, Category>): List<Transaction> {
        val out = mutableListOf<Transaction>()
        val projection = arrayOf(
            Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE,
        )
        context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            projection,
            "${Telephony.Sms.DATE} >= ?",
            arrayOf(sinceMillis.toString()),
            "${Telephony.Sms.DATE} DESC",
        )?.use { c ->
            val iId = c.getColumnIndexOrThrow(Telephony.Sms._ID)
            val iAddr = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val iBody = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val iDate = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
            while (c.moveToNext()) {
                val address = c.getString(iAddr) ?: continue
                if (!isBankSender(address)) continue
                val body = c.getString(iBody) ?: continue
                val parsed = SmsParser.parse(body) ?: continue
                out += Transaction(
                    smsId = c.getLong(iId),
                    timestamp = c.getLong(iDate),
                    amount = parsed.amount,
                    type = parsed.type,
                    merchant = parsed.merchant,
                    account = parsed.account,
                    bank = bankName(address),
                    category = Categorizer.categorize(parsed.merchant, body, parsed.type, overrides),
                    rawBody = body,
                )
            }
        }
        return dedupe(out)
    }

    /** Banks send from alphanumeric headers like "VM-HDFCBK"; people send from phone numbers. */
    private fun isBankSender(address: String) = address.any { it.isLetter() }

    /** "AD-SBIUPI-S" -> "SBIUPI" */
    private fun bankName(address: String) =
        address.uppercase().split('-').maxByOrNull { it.length } ?: address

    /** Some banks send two SMS for one payment (UPI + account alert). Keep one. */
    private fun dedupe(list: List<Transaction>): List<Transaction> {
        val kept = mutableListOf<Transaction>()
        for (t in list.sortedByDescending { it.timestamp }) {
            val dup = kept.any {
                it.type == t.type && it.amount == t.amount && abs(it.timestamp - t.timestamp) < 3 * 60_000
            }
            if (!dup) kept += t
        }
        return kept
    }
}
