package com.spendly.app.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class MerchantNormalizerTest {

    @Test
    fun normalize_handlesKnownIndianMerchants() {
        assertEquals("Swiggy", MerchantNormalizer.normalize("swiggy@axisbank"))
        assertEquals("Swiggy", MerchantNormalizer.normalize("SWIGGY BANGALORE IN"))
        assertEquals("Zomato", MerchantNormalizer.normalize("zomato@hdfcbank"))
        assertEquals("Amazon", MerchantNormalizer.normalize("amazonpay@apl"))
        assertEquals("Amazon", MerchantNormalizer.normalize("AMZN Mktp IN*12345"))
        assertEquals("Uber", MerchantNormalizer.normalize("uber@icici"))
        assertEquals("Jio", MerchantNormalizer.normalize("RELIANCE JIO INFOCOMM"))
        assertEquals("Blinkit", MerchantNormalizer.normalize("grofers india pvt ltd"))
        assertEquals("Netflix", MerchantNormalizer.normalize("netflix entertainment"))
    }

    @Test
    fun normalize_cleansHandlesAndCorporateNoise() {
        assertEquals("Rahul Sharma", MerchantNormalizer.normalize("rahul.sharma@okaxis"))
        assertEquals("Starbucks", MerchantNormalizer.normalize("Starbucks Coffee Pvt Ltd"))
        assertEquals("Blue Tokai Coffee", MerchantNormalizer.normalize("Blue Tokai Coffee Pvt Ltd"))
        assertEquals("Unknown Merchant", MerchantNormalizer.normalize(null))
        assertEquals("Unknown Merchant", MerchantNormalizer.normalize("   "))
    }
}
