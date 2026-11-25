package com.healthdekho.ims.service;
import com.healthdekho.ims.dto.ApiDtos.ProductResponse;
import com.healthdekho.ims.entity.*;
import com.healthdekho.ims.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class ProductService {
    private final InsuranceProductRepository productRepository;
    private final CustomerPolicyRepository policyRepository;

    public List<ProductResponse> getAvailableProducts(Customer customer) {
        Set<Long> ownedProductIds = policyRepository.findByCustomerCustomerIdOrderByCreatedAtDesc(customer.getCustomerId())
                .stream().map(cp -> cp.getProduct().getProductId()).collect(Collectors.toSet());
        int age = Period.between(customer.getDateOfBirth(), LocalDate.now()).getYears();
        return productRepository.findByActiveTrueOrderByProductIdAsc().stream()
                .filter(pr -> !ownedProductIds.contains(pr.getProductId()))
                .map(pr -> new ProductResponse(
                        pr.getProductId(), pr.getProductCode(), pr.getProductName(), pr.getPolicyType(),
                        pr.getDescription(), pr.getTermMonths(), pr.getBasePremiumAmount(), pr.getPremiumFrequency(),
                        pr.getSumInsured(), pr.getMinEntryAge(), pr.getMaxEntryAge(),
                        age >= pr.getMinEntryAge() && age <= pr.getMaxEntryAge()))
                .toList();
    }
}