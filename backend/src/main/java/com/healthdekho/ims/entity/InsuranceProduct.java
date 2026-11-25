package com.healthdekho.ims.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity @Table(name = "insurance_product")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InsuranceProduct {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "product_id") private Long productId;
    @Column(name = "product_code", nullable = false, unique = true) private String productCode;
    @Column(name = "product_name", nullable = false) private String productName;
    @Column(name = "policy_type", nullable = false) private String policyType;
    @Column(nullable = false, columnDefinition = "TEXT") private String description;
    @Column(name = "term_months", nullable = false) private Integer termMonths;
    @Column(name = "base_premium_amount", nullable = false) private BigDecimal basePremiumAmount;
    @Column(name = "premium_frequency", nullable = false) private String premiumFrequency;
    @Column(name = "sum_insured", nullable = false) private BigDecimal sumInsured;
    @Column(name = "min_entry_age", nullable = false) private Integer minEntryAge;
    @Column(name = "max_entry_age", nullable = false) private Integer maxEntryAge;
    @Column(nullable = false) private Boolean active;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}