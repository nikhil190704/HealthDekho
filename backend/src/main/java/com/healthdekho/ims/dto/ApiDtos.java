package com.healthdekho.ims.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public final class ApiDtos {
    private ApiDtos() {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    public record CustomerProfile(Long customerId, String fullName, String email, LocalDate dateOfBirth, int age, String phoneNumber, String city) {}
    public record LoginResponse(String token, String tokenType, long expiresInSeconds, CustomerProfile customer) {}

    public record ProductResponse(Long productId, String productCode, String productName, String policyType, String description,
                                  int termMonths, BigDecimal basePremiumAmount, String premiumFrequency, BigDecimal sumInsured,
                                  int minEntryAge, int maxEntryAge, boolean eligibleForCustomerAge) {}

    public record PolicySummaryResponse(Long policyId, String policyNumber, String productCode, String policyName, String policyType,
                                        int policyTermMonths, LocalDate startDate, LocalDate maturityDate, String premiumFrequency,
                                        BigDecimal premiumAmount, BigDecimal totalPremiumPayable, BigDecimal sumInsured,
                                        String policyStatus, String customerName, LocalDate customerDob) {}

    public record RegisterPolicyRequest(@NotNull Long productId, LocalDate startDate) {}

    public record InstallmentResponse(Long installmentId, int installmentNumber, LocalDate dueDate, LocalDate gracePeriodEndDate,
                                      BigDecimal scheduledAmount, BigDecimal paidAmount, BigDecimal penaltyAmount,
                                      BigDecimal remainingPrincipal, BigDecimal applicablePenaltyToday, BigDecimal totalDueToday,
                                      String installmentStatus, LocalDateTime paidAt) {}

    public record PaymentLedgerResponse(Long paymentId, String transactionReference, int installmentNumber, LocalDate paymentDate,
                                        BigDecimal amountPaid, BigDecimal penaltyAmount, BigDecimal totalCharged,
                                        String paymentMode, String paymentStatus) {}

    public record PolicyPaymentOverviewResponse(PolicySummaryResponse policy, BigDecimal totalPremiumPayable, BigDecimal totalPaid,
                                                BigDecimal remainingBalance, List<InstallmentResponse> installments,
                                                List<PaymentLedgerResponse> paymentHistory) {}

    public record ProcessPaymentRequest(@NotBlank String policyNumber, @NotNull Long installmentId,
                                        @NotNull @DecimalMin("0.01") BigDecimal amount, @NotBlank String paymentMode) {}

    public record PaymentReceiptResponse(String transactionReference, String policyNumber, String policyStatus,
                                         int installmentNumber, String installmentStatus, BigDecimal principalPaid,
                                         BigDecimal penaltyCharged, BigDecimal totalCharged, BigDecimal remainingInstallmentBalance,
                                         BigDecimal remainingPolicyBalance, LocalDate paymentDate, String paymentMode) {}

    public record RecommendationRequest(@NotNull LocalDate dateOfBirth, @NotBlank @Size(max = 1000) String insuranceGoals,
                                        Integer preferredTermMonths, String preferredPolicyType) {}

    public record RecommendedPlanItem(int rankOrder, Long productId, String productCode, String productName, String policyType,
                                      int termMonths, BigDecimal premiumAmount, String premiumFrequency, BigDecimal sumInsured,
                                      int suitabilityScore, String suitabilityLabel, String reason) {}

    public record RecommendationResponse(Long recommendationRequestId, int calculatedAge, String aiSource,
                                         List<RecommendedPlanItem> recommendations) {}

    public record PagedResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}

    public record ErrorResponse(LocalDateTime timestamp, int status, String error, String message, String path, List<String> details) {}
}
