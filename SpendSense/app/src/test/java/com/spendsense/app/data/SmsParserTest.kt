package com.spendsense.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmsParserTest {

    private fun check(sms: String, amount: Double, type: TxnType, merchant: String, category: Category) {
        val p = SmsParser.parse(sms) ?: error("Not parsed: $sms")
        assertEquals(amount, p.amount, 0.001)
        assertEquals(type, p.type)
        assertEquals(merchant, p.merchant)
        assertEquals(category, Categorizer.categorize(p.merchant, sms, p.type, emptyMap()))
    }

    @Test fun hdfcUpi() = check(
        "Sent Rs.250.00\nFrom HDFC Bank A/C *1234\nTo SWIGGY\nOn 12/10/26\nRef 628512345678\nNot You?\nCall 18002586161/SMS BLOCK UPI to 7308080808",
        250.0, TxnType.DEBIT, "SWIGGY", Category.FOOD)

    @Test fun hdfcCard() = check(
        "Spent Rs.1,499.00 On HDFC Bank Card 5678 At AMAZON PAY INDIA On 2026-10-11:14:22:10 Not You? Call 18002586161",
        1499.0, TxnType.DEBIT, "AMAZON PAY INDIA", Category.SHOPPING)

    @Test fun iciciUpi() = check(
        "ICICI Bank Acct XX123 debited for Rs 349.00 on 12-Oct-26; Blinkit credited. UPI:628598765432. Call 18002662 for dispute. SMS BLOCK 123 to 9215676766.",
        349.0, TxnType.DEBIT, "Blinkit", Category.GROCERIES)

    @Test fun sbiUpi() = check(
        "Dear UPI user A/C X4321 debited by 180.0 on date 10Oct26 trf to UBER INDIA Refno 628411112222. If not u? call 1800111109. -SBI",
        180.0, TxnType.DEBIT, "UBER INDIA", Category.TRANSPORT)

    @Test fun axisVpa() = check(
        "INR 649.00 debited from A/c no. XX8899 on 09-10-26 to VPA netflix.upi@icici (UPI Ref no 628400001111). Avl Bal INR 12,345.67",
        649.0, TxnType.DEBIT, "netflix.upi", Category.ENTERTAINMENT)

    @Test fun balanceFirst() = check(
        "Avl Bal: INR 10,000.00. INR 500.00 spent on Kotak Card x1111 at DOMINOS PIZZA on 08/10/2026",
        500.0, TxnType.DEBIT, "DOMINOS PIZZA", Category.FOOD)

    @Test fun atm() = check(
        "Rs.2000.00 withdrawn at ATM S1AN0123 from A/c XX1234 on 07-10-26. Avl Bal Rs.8000",
        2000.0, TxnType.DEBIT, "ATM S1AN0123", Category.CASH)

    @Test fun salaryCredit() = check(
        "Your A/c XX1234 is credited with Rs 85,000.00 on 01-10-26 by NEFT from ACME TECH PVT LTD. Avl Bal Rs 1,02,000.00",
        85000.0, TxnType.CREDIT, "ACME TECH PVT LTD", Category.INCOME)

    @Test fun upiCredit() = check(
        "Rs 500.00 credited to A/c XX1234 from VPA rahul.k@okaxis on 05-10-26 (UPI Ref 628412340000)",
        500.0, TxnType.CREDIT, "rahul.k", Category.INCOME)

    @Test fun ignoresOtp() = assertNull(SmsParser.parse("123456 is your OTP for txn of Rs 500 at AMAZON. Do not share."))
    @Test fun ignoresDue() = assertNull(SmsParser.parse("Your credit card bill of Rs 12,000 is due on 15-10-26. Min Amt Due Rs 600."))
    @Test fun ignoresCardPayment() = assertNull(SmsParser.parse("We have received your payment of Rs 12,000 towards your credit card XX5678. Thank you."))
    @Test fun ignoresPromo() = assertNull(SmsParser.parse("You are eligible for a pre-approved loan of Rs 5,00,000. Apply now!"))
}
