package com.healthdekho.ims.service;
import com.healthdekho.ims.dto.ApiDtos.*;
import com.healthdekho.ims.entity.*;
import com.healthdekho.ims.exception.GlobalExceptionHandler.ApiException;
import com.healthdekho.ims.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service @RequiredArgsConstructor
public class PolicyService {
    private final CustomerPolicyRepository policyRepository;
    private final InsuranceProductRepository productRepository;
    private final PremiumInstallmentRepository installmentRepository;

    public List<PolicySummaryResponse> getMyPolicies(Customer customer) {
        return policyRepository.findByCustomerCustomerIdOrderByCreatedAtDesc(customer.getCustomerId()).stream().map(this::toSummary).toList();
    }

    public PagedResponse<PolicySummaryResponse> searchMyPolicies(Customer customer, String policyNumber, String customerName, LocalDate dob, int page, int size) {
        String pNum = (policyNumber == null || policyNumber.isBlank()) ? null : policyNumber.trim();
        String cName = (customerName == null || customerName.isBlank()) ? null : customerName.trim();
        if (pNum == null && cName == null && dob == null) throw new ApiException(HttpStatus.BAD_REQUEST, "Provide at least one search filter");
        Page<CustomerPolicy> res = policyRepository.searchOwnedPolicies(customer.getCustomerId(), pNum, cName, dob, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50)));
        return new PagedResponse<>(res.getContent().stream().map(this::toSummary).toList(), res.getNumber(), res.getSize(), res.getTotalElements(), res.getTotalPages());
    }

    @Transactional
    public PolicySummaryResponse registerPolicy(Customer customer, RegisterPolicyRequest req) {
        InsuranceProduct pr = productRepository.findById(req.productId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));
        if (!Boolean.TRUE.equals(pr.getActive())) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Product is inactive");
        if (policyRepository.existsByCustomerCustomerIdAndProductProductId(customer.getCustomerId(), pr.getProductId())) throw new ApiException(HttpStatus.CONFLICT, "Already registered for " + pr.getProductName());
        int age = Period.between(customer.getDateOfBirth(), LocalDate.now()).getYears();
        if (age < pr.getMinEntryAge() || age > pr.getMaxEntryAge()) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Age " + age + " outside eligibility (" + pr.getMinEntryAge() + "-" + pr.getMaxEntryAge() + ")");
        LocalDate start = req.startDate() != null ? req.startDate() : LocalDate.now();
        if (start.isBefore(LocalDate.now().minusDays(1))) throw new ApiException(HttpStatus.BAD_REQUEST, "Start date cannot be in the past");
        int step = switch (pr.getPremiumFrequency().toUpperCase()) { case "MONTHLY" -> 1; case "QUARTERLY" -> 3; case "YEARLY" -> 12; default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid frequency"); };
        int count = Math.max(1, pr.getTermMonths() / step);
        BigDecimal total = pr.getBasePremiumAmount().multiply(BigDecimal.valueOf(count)).setScale(2, RoundingMode.HALF_UP);
        String pNum; do { pNum = "POL-HD-" + LocalDate.now().getYear() + "-" + ThreadLocalRandom.current().nextInt(1000, 9999); } while (policyRepository.findByPolicyNumberIgnoreCase(pNum).isPresent());
        CustomerPolicy saved = policyRepository.save(CustomerPolicy.builder().policyNumber(pNum).customer(customer).product(pr).startDate(start).maturityDate(start.plusMonths(pr.getTermMonths())).premiumFrequency(pr.getPremiumFrequency()).premiumAmount(pr.getBasePremiumAmount()).totalPremiumPayable(total).sumInsured(pr.getSumInsured()).policyStatus("ACTIVE").build());
        List<PremiumInstallment> list = new ArrayList<>();
        for (int i = 0; i < count; i++) { LocalDate due = start.plusMonths((long) i * step); list.add(PremiumInstallment.builder().policy(saved).installmentNumber(i + 1).dueDate(due).gracePeriodEndDate(due.plusDays(15)).scheduledAmount(pr.getBasePremiumAmount()).paidAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)).penaltyAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)).installmentStatus("PENDING").build()); }
        installmentRepository.saveAll(list);
        return toSummary(saved);
    }

    public PolicySummaryResponse toSummary(CustomerPolicy cp) {
        return new PolicySummaryResponse(cp.getPolicyId(), cp.getPolicyNumber(), cp.getProduct().getProductCode(), cp.getProduct().getProductName(), cp.getProduct().getPolicyType(), cp.getProduct().getTermMonths(), cp.getStartDate(), cp.getMaturityDate(), cp.getPremiumFrequency(), cp.getPremiumAmount(), cp.getTotalPremiumPayable(), cp.getSumInsured(), cp.getPolicyStatus(), cp.getCustomer().getFullName(), cp.getCustomer().getDateOfBirth());
    }
}
