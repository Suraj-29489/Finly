package com.personalexpensetracker.data.notification.category

import com.personalexpensetracker.domain.model.Category

/**
 * Resolves a merchant name from a parsed transaction into an appropriate
 * expense category from the EXISTING Finly category system.
 *
 * Uses the same [Category] constants defined in the domain layer:
 * Food, Transportation, Shopping, Bills, Entertainment, Health,
 * Education, Travel, Other.
 *
 * ## Strategy
 * 1. Known merchant → direct category mapping
 * 2. Keyword matching against merchant name
 * 3. Safe fallback to "Other" when confidence is insufficient
 *
 * Does NOT create a second category system.
 */
object MerchantCategoryResolver {

    /**
     * Well-known merchants mapped to their categories.
     * Case-insensitive matching is used.
     */
    private val KNOWN_MERCHANTS: Map<String, String> = buildMap {
        // Food & Dining
        listOf(
            "swiggy", "zomato", "dominos", "domino's", "pizza hut",
            "mcdonald", "mcdonalds", "mcdonald's", "kfc", "burger king",
            "subway", "starbucks", "dunkin", "barbeque nation",
            "haldiram", "haldirams", "bikanervala", "wow momo",
            "box8", "faasos", "eatfit", "licious", "bigbasket",
            "blinkit", "zepto", "instamart", "jiomart", "grofers",
            "dmart", "nature's basket", "freshmenu", "behrouz"
        ).forEach { put(it, Category.FOOD) }

        // Transportation
        listOf(
            "uber", "ola", "rapido", "meru", "namma yatri",
            "metro", "irctc", "redbus", "abhibus",
            "petrol", "hp petrol", "iocl", "bpcl",
            "toll", "fastag", "paytm fastag",
            "parking", "yulu", "bounce", "vogo"
        ).forEach { put(it, Category.TRANSPORTATION) }

        // Shopping
        listOf(
            "amazon", "flipkart", "myntra", "ajio", "nykaa",
            "meesho", "snapdeal", "tata cliq", "reliance digital",
            "croma", "vijay sales", "decathlon", "ikea",
            "h&m", "zara", "uniqlo", "lenskart",
            "boat", "noise", "apple", "samsung",
            "lifestyle", "shoppers stop", "westside",
            "pantaloons", "max fashion", "fbb"
        ).forEach { put(it, Category.SHOPPING) }

        // Bills & Utilities
        listOf(
            "electricity", "bescom", "tata power", "adani electricity",
            "water bill", "gas bill", "piped gas",
            "airtel", "jio", "vi", "vodafone", "bsnl",
            "mobile recharge", "dth recharge", "tata sky",
            "broadband", "act fibernet", "hathway",
            "insurance", "lic", "sbi life", "hdfc life",
            "rent", "maintenance", "society"
        ).forEach { put(it, Category.BILLS) }

        // Entertainment
        listOf(
            "netflix", "amazon prime", "hotstar", "disney+",
            "spotify", "apple music", "youtube premium",
            "zee5", "sonyliv", "jiocinema", "mxplayer",
            "bookmyshow", "paytm insider", "pvr", "inox",
            "gaming", "steam", "play store", "google play"
        ).forEach { put(it, Category.ENTERTAINMENT) }

        // Health
        listOf(
            "apollo", "pharmeasy", "1mg", "netmeds",
            "medplus", "practo", "lybrate",
            "hospital", "clinic", "diagnostic",
            "pharmacy", "medical", "dr.", "doctor",
            "gym", "cult.fit", "cultfit", "healthify"
        ).forEach { put(it, Category.HEALTH) }

        // Education
        listOf(
            "coursera", "udemy", "unacademy", "byju",
            "byjus", "vedantu", "physicswallah",
            "school", "college", "university", "tuition",
            "books", "kindle", "audible",
            "exam", "test series"
        ).forEach { put(it, Category.EDUCATION) }

        // Travel
        listOf(
            "makemytrip", "goibibo", "cleartrip",
            "booking.com", "oyo", "airbnb", "treebo",
            "air india", "indigo", "spicejet", "vistara",
            "hotel", "resort", "lodge",
            "flight", "airline", "airport",
            "yatra", "ixigo", "ease my trip"
        ).forEach { put(it, Category.TRAVEL) }
    }

    /**
     * Keyword-based category hints for when exact merchant matching fails.
     */
    private val CATEGORY_KEYWORDS: Map<String, List<String>> = mapOf(
        Category.FOOD to listOf("food", "restaurant", "cafe", "bakery", "snack", "meal", "dinner", "lunch", "breakfast", "grocery", "supermarket"),
        Category.TRANSPORTATION to listOf("cab", "taxi", "ride", "fuel", "diesel", "bus", "train", "auto", "rickshaw"),
        Category.SHOPPING to listOf("store", "shop", "mart", "mall", "purchase", "order", "electronics"),
        Category.BILLS to listOf("bill", "recharge", "prepaid", "postpaid", "subscription", "emi", "loan", "premium"),
        Category.ENTERTAINMENT to listOf("movie", "cinema", "theatre", "game", "play", "music", "stream"),
        Category.HEALTH to listOf("health", "medical", "medicine", "wellness", "fitness", "dental", "optical"),
        Category.EDUCATION to listOf("course", "class", "study", "learn", "tutorial", "exam", "education"),
        Category.TRAVEL to listOf("travel", "trip", "tour", "vacation", "holiday", "visa", "passport")
    )

    /**
     * Resolves the category for a given merchant name.
     *
     * @param merchantName The merchant/payee name from the parsed transaction.
     * @return The resolved category string (one of the existing Category constants).
     */
    fun resolveCategory(merchantName: String?): String {
        if (merchantName.isNullOrBlank() || merchantName == "Unknown Merchant") {
            return Category.OTHER
        }

        val lowerMerchant = merchantName.lowercase().trim()

        // 1. Try exact known merchant match
        KNOWN_MERCHANTS.entries.firstOrNull { (merchant, _) ->
            lowerMerchant.contains(merchant)
        }?.let { return it.value }

        // 2. Try keyword-based category matching
        CATEGORY_KEYWORDS.entries.firstOrNull { (_, keywords) ->
            keywords.any { keyword -> lowerMerchant.contains(keyword) }
        }?.let { return it.key }

        // 3. Fallback
        return Category.OTHER
    }
}
