package com.spendsense.app.data

/**
 * Turns a bank SMS into a transaction, or returns null if the SMS is not a
 * completed debit/credit (OTPs, due reminders, promos, failed txns, etc.).
 * Pure Kotlin with no Android dependencies, so it is unit-testable.
 */
object SmsParser {

    data class Parsed(
        val amount: Double,
        val type: TxnType,
        val merchant: String,
        val account: String?,
    )

    private val opts = setOf(RegexOption.IGNORE_CASE)

    private fun wordsRegex(words: List<String>) =
        Regex("""\b(?:${words.joinToString("|") { Regex.escape(it) }})\b""", opts)

    private val IGNORE = wordsRegex(
        listOf(
            "otp", "one time password", "verification code",
            "will be debited", "will be credited", "to be debited",
            "is due", "due on", "due by", "min amt due", "minimum amount due",
            "total amt due", "total amount due",
            "has requested", "requested money", "collect request",
            "pre-approved", "preapproved", "apply now", "eligible for",
            "failed", "declined", "unsuccessful",
            "received your payment", "payment received", "thank you for your payment",
            "mandate created", "mandate is created", "mandate registered",
        )
    )

    private val DEBIT = wordsRegex(
        listOf(
            "debited", "spent", "sent", "paid", "withdrawn", "purchase", "deducted",
            "txn of", "transaction of", "payment of", "transferred", "used for", "charged",
        )
    )
    private val CREDIT = wordsRegex(
        listOf("credited", "received", "deposited", "refund", "refunded", "reversed")
    )

    private const val NUM = """([0-9][0-9,]*(?:\.[0-9]{1,2})?)"""
    private val CURRENCY_AMOUNT = Regex("""(?:rs\.?|inr|₹)\s*$NUM""", opts)
    private val VERB_AMOUNT =
        Regex("""\b(?:debited|credited)\s+(?:by|for|with)\s+(?:(?:rs\.?|inr|₹)\s*)?$NUM""", opts)
    private val BALANCE_TAIL =
        Regex("""(?:bal(?:ance)?|lmt|limit)[\s:.\-]*(?:is)?[\s:.\-]*$""", opts)

    private val ACCOUNT = Regex("""\b(?:a/c|acct|account|ac|card)\b[^0-9\n]{0,15}?(\d{3,6})""", opts)

    // Where a merchant name ends: punctuation, end of text, or a connector word.
    private const val STOP =
        """(?=\s*(?:[;,(\[]|\.(?:\s|$)|$)|\s+(?:on|ref|refno|upi|via|using|for|avl|avbl|dated|from|by|thru|through|txn|not|if|is|has|with|and)\b)"""
    private const val NAME = """([A-Za-z0-9&._'@/ \-]{2,40}?)"""

    private val DEBIT_MERCHANT = listOf(
        Regex(""";\s*$NAME\s+credited""", opts), // ICICI: "...; SWIGGY credited."
        Regex("""\b(?:at|to|towards)\s+(?:vpa\s+)?$NAME$STOP""", opts),
        Regex("""\binfo[:\s\-]+$NAME$STOP""", opts),
    )
    private val CREDIT_MERCHANT = listOf(
        Regex("""\bfrom\s+(?:vpa\s+)?$NAME$STOP""", opts),
        Regex("""\bby\s+(?:vpa\s+)?$NAME$STOP""", opts),
        Regex("""\binfo[:\s\-]+$NAME$STOP""", opts),
    )

    fun parse(body: String): Parsed? {
        val text = body.replace(Regex("\\s+"), " ").trim()
        if (IGNORE.containsMatchIn(text)) return null

        val debitAt = DEBIT.find(text)?.range?.first ?: Int.MAX_VALUE
        val creditAt = CREDIT.find(text)?.range?.first ?: Int.MAX_VALUE
        if (debitAt == Int.MAX_VALUE && creditAt == Int.MAX_VALUE) return null
        val type = if (debitAt <= creditAt) TxnType.DEBIT else TxnType.CREDIT

        val amount = extractAmount(text) ?: return null
        if (amount <= 0.0) return null

        val account = ACCOUNT.find(text)?.groupValues?.get(1)
        val merchant = extractMerchant(text, type)
            ?: if (type == TxnType.CREDIT) "Credit" else "Unknown"
        return Parsed(amount, type, merchant, account)
    }

    private fun extractAmount(text: String): Double? {
        VERB_AMOUNT.find(text)?.let { return toAmount(it.groupValues[1]) }
        for (m in CURRENCY_AMOUNT.findAll(text)) {
            val before = text.substring(maxOf(0, m.range.first - 20), m.range.first)
            if (BALANCE_TAIL.containsMatchIn(before)) continue // skip "Avl Bal Rs 12,000"
            return toAmount(m.groupValues[1])
        }
        return null
    }

    private fun toAmount(s: String) = s.replace(",", "").trimEnd('.').toDoubleOrNull()

    private fun extractMerchant(text: String, type: TxnType): String? {
        val patterns = if (type == TxnType.DEBIT) DEBIT_MERCHANT else CREDIT_MERCHANT
        for (p in patterns) {
            for (m in p.findAll(text)) {
                val cleaned = clean(m.groupValues[1]) ?: continue
                return cleaned
            }
        }
        return null
    }

    private val BAD_START = Regex("""^(?:(?:a/c|ac|acct|account|card|your|you|xx)\b|\*)""", opts)

    private fun clean(raw: String): String? {
        var s = raw.trim().trim('-', '.', ' ')
        if (s.contains('@')) s = s.substringBefore('@') // VPA: zomato.payu@axis -> zomato.payu
        s = s.replace(Regex("\\s+"), " ").trim()
        if (s.length < 2 || s.none { it.isLetter() } || BAD_START.containsMatchIn(s)) return null
        return s.take(30)
    }
}
