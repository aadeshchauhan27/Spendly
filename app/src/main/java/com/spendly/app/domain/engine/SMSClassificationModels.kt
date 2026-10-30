package com.spendly.app.domain.engine

import com.spendly.app.domain.model.Transaction

enum class ClassificationConfidence {
    HIGH,
    MEDIUM,
    LOW
}

enum class NoiseCategory {
    NONE,
    OTP_AUTHENTICATION,
    PROMOTIONAL_OFFER,
    BILL_PAYMENT_REMINDER,
    KYC_ACCOUNT_SECURITY,
    INVESTMENT_INFO,
    ORDER_DELIVERY_NOTIFICATION,
    FAILED_OR_DECLINED
}

enum class RefundState {
    NOT_A_REFUND,
    REFUND_INITIATED,
    REFUND_COMPLETED
}

data class ClassificationResult(
    val isFinancialTransaction: Boolean,
    val confidence: ClassificationConfidence,
    val noiseCategory: NoiseCategory = NoiseCategory.NONE,
    val transaction: Transaction? = null,
    val refundState: RefundState = RefundState.NOT_A_REFUND,
    val accountEnding: String? = null,
    val availableBalancePaise: Long? = null,
    val rejectionReason: String? = null
)
