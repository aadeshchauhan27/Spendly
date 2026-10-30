package com.spendly.app.domain.engine

object MerchantNormalizer {

    private val KNOWN_MERCHANT_PATTERNS = listOf(
        Regex("(?i).*swiggy.*") to "Swiggy",
        Regex("(?i).*zomato.*") to "Zomato",
        Regex("(?i).*(amazon pay|amazon|amzn).*") to "Amazon",
        Regex("(?i).*flipkart.*") to "Flipkart",
        Regex("(?i).*myntra.*") to "Myntra",
        Regex("(?i).*ajio.*") to "Ajio",
        Regex("(?i).*meesho.*") to "Meesho",
        Regex("(?i).*nykaa.*") to "Nykaa",
        Regex("(?i).*(blinkit|grofers).*") to "Blinkit",
        Regex("(?i).*(zepto|kirana).*") to "Zepto",
        Regex("(?i).*(bigbasket|bb daily).*") to "BigBasket",
        Regex("(?i).*instamart.*") to "Instamart",
        Regex("(?i).*uber.*") to "Uber",
        Regex("(?i).*(ola|ani tech).*") to "Ola",
        Regex("(?i).*rapido.*") to "Rapido",
        Regex("(?i).*(jio|reliance jio).*") to "Jio",
        Regex("(?i).*(airtel|bharti airtel).*") to "Airtel",
        Regex("(?i).*(vodafone|idea|\\bvi\\b).*") to "Vi",
        Regex("(?i).*bescom.*") to "BESCOM",
        Regex("(?i).*tata play.*") to "Tata Play",
        Regex("(?i).*(google pay|gpay).*") to "Google Pay",
        Regex("(?i).*(google).*") to "Google",
        Regex("(?i).*phonepe.*") to "PhonePe",
        Regex("(?i).*paytm.*") to "Paytm",
        Regex("(?i).*netflix.*") to "Netflix",
        Regex("(?i).*spotify.*") to "Spotify",
        Regex("(?i).*(bookmyshow|bigtree).*") to "BookMyShow",
        Regex("(?i).*(pvr|inox).*") to "PVR Inox",
        Regex("(?i).*irctc.*") to "IRCTC",
        Regex("(?i).*(makemytrip|mmt).*") to "MakeMyTrip",
        Regex("(?i).*\\b(cred|dreamplug)\\b.*") to "CRED",
        Regex("(?i).*tataneu.*") to "Tata Neu",
        Regex("(?i).*(starbucks|tata starbucks).*") to "Starbucks",
        Regex("(?i).*mcdonald.*") to "McDonald's",
        Regex("(?i).*(domino|jubilant).*") to "Domino's",
        Regex("(?i).*chai point.*") to "Chai Point",
        Regex("(?i).*dmart.*") to "DMart",
        Regex("(?i).*(apollo pharmacy|apollo).*") to "Apollo Pharmacy",
        Regex("(?i).*(pharmeasy).*") to "PharmEasy",
        Regex("(?i).*(1mg|tata 1mg).*") to "Tata 1mg",
        Regex("(?i).*zerodha.*") to "Zerodha",
        Regex("(?i).*groww.*") to "Groww",
        Regex("(?i).*the mills pan shop.*") to "The Mills Pan Shop",
        Regex("(?i).*bagwan fruits.*") to "Bagwan Fruits",
        Regex("(?i).*a1 cafe.*") to "A1 Cafe",
        Regex("(?i).*adobe.*") to "Adobe",
        Regex("(?i).*mahalaxmi medical.*") to "Mahalaxmi Medical",
        Regex("(?i).*ratan raju shinde.*") to "Ratan Raju Shinde",
        Regex("(?i).*mukesh prakash.*") to "Mukesh Prakash",
        Regex("(?i).*angad bihari.*") to "Angad Bihari",
        Regex("(?i).*monika singh.*") to "Monika Singh",
        Regex("(?i).*jeeves.*") to "Jeeves Protection",
        Regex("(?i).*hdfc bank.*") to "HDFC Bank",
        Regex("(?i).*icici bank.*") to "ICICI Bank",
        Regex("(?i).*axis bank.*") to "Axis Bank",
        Regex("(?i).*kotak bank.*") to "Kotak Bank",
        Regex("(?i).*idfc.*") to "IDFC FIRST Bank",
        Regex("(?i).*yes bank.*") to "YES Bank",
        Regex("(?i).*sbi.*") to "SBI"
    )

    /**
     * Normalizes a raw UPI VPA or merchant title into a clean merchant name.
     */
    fun normalize(rawInput: String?): String {
        if (rawInput.isNullOrBlank()) return "Unknown Merchant"

        val cleaned = rawInput.trim()

        // 1. Check known merchant regex patterns
        for ((pattern, normalizedName) in KNOWN_MERCHANT_PATTERNS) {
            if (pattern.matches(cleaned)) {
                return normalizedName
            }
        }

        // 2. Strip VPA handles (e.g. "name@okhdfcbank" -> "Name")
        if (cleaned.contains("@")) {
            val handlePrefix = cleaned.substringBefore("@")
                .replace(Regex("[^a-zA-Z0-9 ]"), " ")
                .trim()
            if (handlePrefix.isNotEmpty() && !handlePrefix.matches(Regex("^[0-9]+$"))) {
                return capitalizeWords(handlePrefix)
            }
        }

        // 3. Remove common corporate noise suffixes (Pvt Ltd, India, Limited, Retail)
        val stripped = cleaned
            .replace(Regex("(?i)\\b(pvt|ltd|limited|private|india|technologies|services|store|enterprises|retail|tech)\\b"), "")
            .replace(Regex("[^a-zA-Z0-9 &'-]"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")

        return if (stripped.isNotBlank()) capitalizeWords(stripped) else capitalizeWords(cleaned)
    }

    private fun capitalizeWords(input: String): String {
        return input.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }
}
