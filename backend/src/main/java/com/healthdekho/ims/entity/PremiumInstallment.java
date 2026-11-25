package com.healthdekho.ims.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
@Entity @Table(name = "premium_installment")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PremiumInstallment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "installment_id") private Long installmentId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "policy_id", nullable = false) private CustomerPolicy policy;
    @Column(name = "installment_number", nullable = false) private Integer installmentNumber;
    @Column(name = "due_date", nullable = false) private LocalDate dueDate;
    @Column(name = "grace_period_end_date", nullable = false) private LocalDate gracePeriodEndDate;
    @Column(name = "scheduled_amount", nullable = false) private BigDecimal scheduledAmount;
    @Column(name = "paid_amount", nullable = false) private BigDecimal paidAmount;
    @Column(name = "penalty_amount", nullable = false) private BigDecimal penaltyAmount;
    @Column(name = "installment_status", nullable = false) private String installmentStatus;
    @Column(name = "paid_at") private LocalDateTime paidAt;
}