package com.healthdekho.ims.service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdekho.ims.dto.ApiDtos.*;
import com.healthdekho.ims.entity.*;
import com.healthdekho.ims.exception.GlobalExceptionHandler.ApiException;
import com.healthdekho.ims.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class RecommendationService {
    private final InsuranceProductRepository productRepository;
    private final CustomerPolicyRepository policyRepository;
    private final AiRecommendationRequestRepository recommendationRepository;
    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final ObjectMapper objectMapper;

    @Value("${healthdekho.ai.enabled:false}")
    private boolean aiEnabled;

    private record AiDraft(String productCode, int suitabilityScore, String reason) {}

    @Transactional
    public RecommendationResponse recommend(Customer customer, RecommendationRequest req) {
        if (req.dateOfBirth().isAfter(LocalDate.now().minusYears(18))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Customer must be at least 18 years old for policy recommendations");
        }
        int age = Period.between(req.dateOfBirth(), LocalDate.now()).getYears();
        Set<Long> ownedIds = policyRepository.findByCustomerCustomerIdOrderByCreatedAtDesc(customer.getCustomerId())
                .stream().map(cp -> cp.getProduct().getProductId()).collect(Collectors.toSet());

        List<InsuranceProduct> eligible = productRepository.findByActiveTrueOrderByProductIdAsc().stream()
                .filter(pr -> !ownedIds.contains(pr.getProductId()))
                .filter(pr -> age >= pr.getMinEntryAge() && age <= pr.getMaxEntryAge())
                .toList();

        if (eligible.isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "No unregistered active insurance products match age " + age);
        }

        String aiSource = "DETERMINISTIC_ENGINE";
        List<AiDraft> drafts = List.of();
        if (aiEnabled && chatClientBuilderProvider.getIfAvailable() != null) {
            try {
                drafts = callSpringAi(eligible, age, req);
                if (!drafts.isEmpty()) aiSource = "SPRING_AI";
            } catch (Exception ignored) {
                drafts = List.of();
            }
        }
        if (drafts.isEmpty()) {
            drafts = buildDeterministicDrafts(eligible, age, req);
        }

        Map<String, InsuranceProduct> byCode = eligible.stream()
                .collect(Collectors.toMap(pr -> pr.getProductCode().toUpperCase(), pr -> pr));
        AiRecommendationRequest auditReq = AiRecommendationRequest.builder()
                .customer(customer).submittedDob(req.dateOfBirth()).calculatedAge(age)
                .insuranceGoals(req.insuranceGoals().trim()).preferredTermMonths(req.preferredTermMonths())
                .preferredPolicyType(req.preferredPolicyType()).aiSource(aiSource).build();

        List<RecommendedPlanItem> items = new ArrayList<>();
        int rank = 1;
        for (AiDraft d : drafts) {
            InsuranceProduct pr = byCode.get(d.productCode().toUpperCase());
            if (pr == null) continue;
            int score = Math.min(99, Math.max(60, d.suitabilityScore()));
            String label = score >= 88 ? "Best Match" : (score >= 76 ? "Strong Fit" : "Good Option");
            auditReq.getItems().add(AiRecommendationItem.builder().request(auditReq).product(pr).rankOrder(rank)
                    .suitabilityScore(score).suitabilityLabel(label).recommendationReason(d.reason()).build());
            items.add(new RecommendedPlanItem(rank, pr.getProductId(), pr.getProductCode(), pr.getProductName(),
                    pr.getPolicyType(), pr.getTermMonths(), pr.getBasePremiumAmount(), pr.getPremiumFrequency(),
                    pr.getSumInsured(), score, label, d.reason()));
            rank++;
        }
        AiRecommendationRequest saved = recommendationRepository.save(auditReq);
        return new RecommendationResponse(saved.getRecommendationRequestId(), age, aiSource, items);
    }

    private List<AiDraft> callSpringAi(List<InsuranceProduct> eligible, int age, RecommendationRequest req) throws Exception {
        String catalog = eligible.stream().map(p -> p.getProductCode() + "|" + p.getProductName() + "|" + p.getPolicyType()
                + "|term=" + p.getTermMonths() + "m|premium=" + p.getBasePremiumAmount() + "|cover=" + p.getSumInsured()).collect(Collectors.joining("\n"));
        String prompt = "You are HealthDekho's insurance advisor. Return ONLY a JSON array of objects with fields productCode, suitabilityScore (60-99), reason. "
                + "Use ONLY productCodes from this catalog:\n" + catalog + "\nCustomer age: " + age + ", goals: " + req.insuranceGoals()
                + ", preferredType: " + req.preferredPolicyType() + ", preferredTermMonths: " + req.preferredTermMonths();
        String raw = chatClientBuilderProvider.getObject().build().prompt().user(prompt).call().content();
        String json = raw.substring(raw.indexOf('['), raw.lastIndexOf(']') + 1);
        return objectMapper.readValue(json, new TypeReference<List<AiDraft>>() {});
    }

    private List<AiDraft> buildDeterministicDrafts(List<InsuranceProduct> eligible, int age, RecommendationRequest req) {
        String goals = req.insuranceGoals().toLowerCase();
        String prefType = req.preferredPolicyType() != null ? req.preferredPolicyType().trim().toUpperCase() : "";
        return eligible.stream().map(pr -> {
            int score = 72;
            if (!prefType.isBlank() && pr.getPolicyType().equalsIgnoreCase(prefType)) score += 16;
            if (req.preferredTermMonths() != null && Math.abs(pr.getTermMonths() - req.preferredTermMonths()) <= 12) score += 8;
            if (goals.contains("family") && pr.getPolicyType().contains("FAMILY")) score += 10;
            if ((goals.contains("life") || goals.contains("tax") || goals.contains("long")) && pr.getPolicyType().contains("TERM")) score += 10;
            if ((goals.contains("critical") || goals.contains("illness") || goals.contains("cancer") || goals.contains("heart")) && pr.getPolicyType().contains("CRITICAL")) score += 10;
            if ((goals.contains("hospital") || goals.contains("health") || goals.contains("medical")) && pr.getPolicyType().contains("HEALTH")) score += 8;
            String reason = "Tailored for age " + age + " yrs: " + pr.getProductName() + " provides INR "
                    + pr.getSumInsured().toPlainString() + " sum insured over " + pr.getTermMonths()
                    + " months (" + pr.getPremiumFrequency().toLowerCase() + " premium INR " + pr.getBasePremiumAmount().toPlainString()
                    + "), aligned with your goal: \"" + req.insuranceGoals().trim() + "\".";
            return new AiDraft(pr.getProductCode(), Math.min(score, 98), reason);
        }).sorted(Comparator.comparingInt(AiDraft::suitabilityScore).reversed()).toList();
    }
}
