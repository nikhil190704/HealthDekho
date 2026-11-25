package com.healthdekho.ims.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.*;
@Entity @Table(name = "ai_recommendation_request")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiRecommendationRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "recommendation_request_id") private Long recommendationRequestId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "customer_id", nullable = false) private Customer customer;
    @Column(name = "submitted_dob", nullable = false) private LocalDate submittedDob;
    @Column(name = "calculated_age", nullable = false) private Integer calculatedAge;
    @Column(name = "insurance_goals", nullable = false, columnDefinition = "TEXT") private String insuranceGoals;
    @Column(name = "preferred_term_months") private Integer preferredTermMonths;
    @Column(name = "preferred_policy_type") private String preferredPolicyType;
    @Column(name = "ai_source", nullable = false) private String aiSource;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true) @Builder.Default private List<AiRecommendationItem> items = new ArrayList<>();
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}