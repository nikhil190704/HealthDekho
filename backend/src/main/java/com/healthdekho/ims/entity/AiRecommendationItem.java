package com.healthdekho.ims.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name = "ai_recommendation_item")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiRecommendationItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "recommendation_item_id") private Long recommendationItemId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "recommendation_request_id", nullable = false) private AiRecommendationRequest request;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "product_id", nullable = false) private InsuranceProduct product;
    @Column(name = "rank_order", nullable = false) private Integer rankOrder;
    @Column(name = "suitability_score", nullable = false) private Integer suitabilityScore;
    @Column(name = "suitability_label", nullable = false) private String suitabilityLabel;
    @Column(name = "recommendation_reason", nullable = false, columnDefinition = "TEXT") private String recommendationReason;
}