package com.healthdekho.ims.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
@Entity @Table(name = "premium_payment")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PremiumPayment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "payment_id") private Long paymentId;
    @Column(name = "transaction_reference", nullable = false, unique = true) private String transactionReference;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "policy_id", nullable = false) private CustomerPolicy policy;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "installment_id", nullable = false) private PremiumInstallment installment;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "customer_id", nullable = false) private Customer customer;
    @Column(name = "payment_date", nullable = false) private LocalDate paymentDate;
    @Column(name = "amount_paid", nullable = false) private BigDecimal amountPaid;
    @Column(name = "penalty_amount", nullable = false) private BigDecimal penaltyAmount;
    @Column(name = "total_charged", nullable = false) private BigDecimal totalCharged;
    @Column(name = "payment_mode", nullable = false) private String paymentMode;
    @Column(name = "payment_status", nullable = false) private String paymentStatus;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}