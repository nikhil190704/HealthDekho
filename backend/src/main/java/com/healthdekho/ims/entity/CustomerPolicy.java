package com.healthdekho.ims.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
@Entity @Table(name = "customer_policy")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CustomerPolicy {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "policy_id") private Long policyId;
    @Column(name = "policy_number", nullable = false, unique = true) private String policyNumber;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "customer_id", nullable = false) private Customer customer;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "product_id", nullable = false) private InsuranceProduct product;
    @Column(name = "start_date", nullable = false) private LocalDate startDate;
    @Column(name = "maturity_date", nullable = false) private LocalDate maturityDate;
    @Column(name = "premium_frequency", nullable = false) private String premiumFrequency;
    @Column(name = "premium_amount", nullable = false) private BigDecimal premiumAmount;
    @Column(name = "total_premium_payable", nullable = false) private BigDecimal totalPremiumPayable;
    @Column(name = "sum_insured", nullable = false) private BigDecimal sumInsured;
    @Column(name = "policy_status", nullable = false) private String policyStatus;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}