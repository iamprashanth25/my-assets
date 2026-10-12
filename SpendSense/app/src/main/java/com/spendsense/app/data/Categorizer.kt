package com.spendsense.app.data

/**
 * Keyword rules for Indian merchants. Order matters: the first matching rule wins.
 * Keywords of 4 chars or fewer must match a whole word (so "cred" won't match "credited").
 */
object Categorizer {

    private val RULES: List<Pair<Category, List<String>>> = listOf(
        Category.CASH to listOf("atm", "cash withdrawal", "cash wdl", "withdrawn"),
        Category.INVESTMENT to listOf(
            "zerodha", "groww", "upstox", "kuvera", "indmoney", "smallcase", "angel one",
            "paytm money", "mutual fund", "sip", "nps", "coin",
        ),
        Category.EMI to listOf("emi", "loan", "bajaj fin", "nach", "ach dr", "home credit"),
        Category.RENT to listOf("rent", "nobroker", "housing", "magicbricks"),
        Category.FOOD to listOf(
            "zomato", "swiggy", "eatsure", "dominos", "domino", "pizza", "mcdonald", "kfc",
            "burger", "starbucks", "cafe", "restaurant", "chaayos", "dunkin", "subway",
            "eatclub", "box8", "faasos", "biryani", "bakery", "haldiram",
        ),
        Category.GROCERIES to listOf(
            "blinkit", "zepto", "bigbasket", "instamart", "dmart", "jiomart", "grofers",
            "reliance fresh", "more retail", "spencers", "milkbasket", "country delight",
            "supermarket", "kirana", "bbnow",
        ),
        Category.SHOPPING to listOf(
            "amazon", "flipkart", "myntra", "ajio", "meesho", "nykaa", "tatacliq", "croma",
            "reliance digital", "decathlon", "ikea", "lenskart", "snapdeal", "firstcry",
            "purplle", "zara", "h&m", "uniqlo",
        ),
        Category.TRAVEL to listOf(
            "irctc", "makemytrip", "goibibo", "cleartrip", "ixigo", "redbus", "indigo",
            "airasia", "akasa", "air india", "vistara", "spicejet", "oyo", "airbnb",
            "booking.com", "agoda", "yatra",
        ),
        Category.TRANSPORT to listOf(
            "uber", "ola", "olacabs", "rapido", "namma yatri", "blusmart", "metro", "fastag",
            "petrol", "fuel", "hpcl", "iocl", "bpcl", "indian oil", "shell", "parking", "yulu",
        ),
        Category.BILLS to listOf(
            "airtel", "jio", "vodafone", "vi", "bsnl", "fibernet", "electricity", "bescom",
            "tneb", "msedcl", "tata power", "adani elec", "bbps", "recharge", "broadband",
            "dth", "tata play", "gas", "water bill", "billdesk", "insurance", "lic",
            "cred", "credit card bill",
        ),
        Category.ENTERTAINMENT to listOf(
            "netflix", "spotify", "hotstar", "disney", "prime video", "youtube", "bookmyshow",
            "pvr", "inox", "sonyliv", "zee5", "jiocinema", "steam", "playstation",
        ),
        Category.HEALTH to listOf(
            "pharmacy", "apollo", "1mg", "pharmeasy", "netmeds", "medplus", "hospital",
            "clinic", "diagnostic", "lab", "practo", "cult", "gym", "healthkart",
        ),
        Category.TRANSFER to listOf("neft", "imps", "rtgs"),
    ).map { (cat, words) -> cat to words.map(::normalize) }

    private fun normalize(s: String) =
        s.lowercase().replace(Regex("[^a-z0-9&]+"), " ").trim()

    private fun matches(padded: String, keyword: String): Boolean =
        if (keyword.length <= 4 && ' ' !in keyword) padded.contains(" $keyword ")
        else padded.contains(keyword)

    private fun ruleFor(text: String): Category? {
        val padded = " ${normalize(text)} "
        return RULES.firstOrNull { (_, words) -> words.any { matches(padded, it) } }?.first
    }

    fun overrideKey(merchant: String) = merchant.trim().lowercase()

    fun categorize(
        merchant: String,
        body: String,
        type: TxnType,
        overrides: Map<String, Category>,
    ): Category {
        overrides[overrideKey(merchant)]?.let { return it }
        if (type == TxnType.CREDIT) return Category.INCOME
        return ruleFor(merchant) ?: ruleFor(body) ?: Category.OTHER
    }
}
