package com.spendsense.app.data

enum class TxnType { DEBIT, CREDIT }

enum class Category(val label: String, val emoji: String) {
    FOOD("Food & Dining", "🍔"),
    GROCERIES("Groceries", "🛒"),
    SHOPPING("Shopping", "🛍️"),
    TRANSPORT("Transport & Fuel", "🚗"),
    TRAVEL("Travel", "✈️"),
    BILLS("Bills & Recharges", "💡"),
    ENTERTAINMENT("Entertainment", "🎬"),
    HEALTH("Health & Fitness", "💊"),
    RENT("Rent", "🏠"),
    EMI("EMI & Loans", "🏦"),
    INVESTMENT("Investments", "📈"),
    CASH("Cash Withdrawal", "🏧"),
    TRANSFER("Transfers", "🔁"),
    INCOME("Income", "💰"),
    OTHER("Other", "📦"),
}

data class Transaction(
    val smsId: Long,
    val timestamp: Long,
    val amount: Double,
    val type: TxnType,
    val merchant: String,
    val account: String?,
    val bank: String,
    val category: Category,
    val rawBody: String,
)
