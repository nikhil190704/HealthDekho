package com.healthdekho.ims.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdekho.ims.dto.ApiDtos.*;
import com.healthdekho.ims.entity.*;
import com.healthdekho.ims.exception.GlobalExceptionHandler.ApiException;
import com.healthdekho.ims.repository.*;
import com.healthdekho.ims.security.JwtService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.AuthenticationManager;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthDekhoServicesTest {
    @Mock CustomerRepository customerRepo;
    @Mock InsuranceProductRepository productRepo;
    @Mock CustomerPolicyRepository policyRepo;
    @Mock PremiumInstallmentRepository instRepo;
    @Mock PremiumPaymentRepository payRepo;
    @Mock AiRecommendationRequestRepository recRepo;
    @Mock AuthenticationManager authManager;
    @Mock JwtService jwtService;
    @Mock ObjectProvider<org.springframework.ai.chat.client.ChatClient.Builder> aiProvider;

    private Customer rahul;
    private InsuranceProduct healthProd;

    @BeforeEach void setUp() {
        rahul = Customer.builder().customerId(1L).fullName("Rahul Sharma").email("rahul.sharma@healthdekho.com")
                .passwordHash("hash").dateOfBirth(LocalDate.of(1992, 5, 14)).phoneNumber("9999999999").city("Gurugram").build();
        healthProd = InsuranceProduct.builder().productId(1L).productCode("HD-HLTH-101").productName("Care Shield")
                .policyType("HEALTH").description("Desc").termMonths(12).basePremiumAmount(new BigDecimal("12000.00"))
                .premiumFrequency("YEARLY").sumInsured(new BigDecimal("500000.00")).minEntryAge(18).maxEntryAge(65).active(true).build();
    }

    @Test void authLoginAndProfile() {
        when(customerRepo.findByEmailIgnoreCase("rahul.sharma@healthdekho.com")).thenReturn(Optional.of(rahul));
        when(jwtService.generateToken(1L, "rahul.sharma@healthdekho.com")).thenReturn("jwt-tok");
        when(jwtService.getExpirationSeconds()).thenReturn(86400L);
        AuthService svc = new AuthService(authManager, customerRepo, jwtService);
        LoginResponse res = svc.login(new LoginRequest("rahul.sharma@healthdekho.com", "Password@123"));
        assertEquals("jwt-tok", res.token());
        assertEquals("Rahul Sharma", res.customer().fullName());
    }

    @Test void productsExcludeAlreadyOwned() {
        CustomerPolicy owned = CustomerPolicy.builder().policyId(10L).customer(rahul).product(healthProd).build();
        when(policyRepo.findByCustomerCustomerIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(owned));
        when(productRepo.findByActiveTrueOrderByProductIdAsc()).thenReturn(List.of(healthProd));
        ProductService svc = new ProductService(productRepo, policyRepo);
        assertTrue(svc.getAvailableProducts(rahul).isEmpty());
    }

    @Test void registerAndSearchPolicy() {
        when(productRepo.findById(1L)).thenReturn(Optional.of(healthProd));
        when(policyRepo.existsByCustomerCustomerIdAndProductProductId(1L, 1L)).thenReturn(false);
        when(policyRepo.findByPolicyNumberIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(policyRepo.save(any())).thenAnswer(inv -> { CustomerPolicy p = inv.getArgument(0); p.setPolicyId(11L); return p; });
        PolicyService svc = new PolicyService(policyRepo, productRepo, instRepo);
        PolicySummaryResponse created = svc.registerPolicy(rahul, new RegisterPolicyRequest(1L, LocalDate.now()));
        assertEquals("ACTIVE", created.policyStatus());
        assertThrows(ApiException.class, () -> svc.searchMyPolicies(rahul, null, null, null, 0, 10));
        when(policyRepo.searchOwnedPolicies(eq(1L), eq("POL"), isNull(), isNull(), any())).thenReturn(new PageImpl<>(List.of()));
        assertEquals(0, svc.searchMyPolicies(rahul, "POL", null, null, 0, 10).totalElements());
    }

    @Test void paymentWithinGraceAndLatePenalty() {
        PolicyService policySvc = new PolicyService(policyRepo, productRepo, instRepo);
        PaymentService paySvc = new PaymentService(policyRepo, instRepo, payRepo, policySvc);
        CustomerPolicy pol = CustomerPolicy.builder().policyId(2L).policyNumber("POL-HD-2026-1002").customer(rahul)
                .product(healthProd).startDate(LocalDate.now().minusMonths(2)).maturityDate(LocalDate.now().plusMonths(10))
                .premiumFrequency("QUARTERLY").premiumAmount(new BigDecimal("1000.00")).totalPremiumPayable(new BigDecimal("1000.00"))
                .sumInsured(new BigDecimal("500000.00")).policyStatus("ACTIVE").build();
        PremiumInstallment overdueInst = PremiumInstallment.builder().installmentId(5L).policy(pol).installmentNumber(1)
                .dueDate(LocalDate.now().minusDays(20)).gracePeriodEndDate(LocalDate.now().minusDays(5))
                .scheduledAmount(new BigDecimal("1000.00")).paidAmount(BigDecimal.ZERO).penaltyAmount(BigDecimal.ZERO)
                .installmentStatus("OVERDUE").build();
        when(policyRepo.findByPolicyNumberIgnoreCase("POL-HD-2026-1002")).thenReturn(Optional.of(pol));
        when(instRepo.findByIdForUpdate(5L)).thenReturn(Optional.of(overdueInst));
        when(instRepo.findByPolicyPolicyIdOrderByInstallmentNumberAsc(2L)).thenReturn(List.of(overdueInst));
        PaymentReceiptResponse receipt = paySvc.processPayment(rahul, new ProcessPaymentRequest("POL-HD-2026-1002", 5L, new BigDecimal("1000.00"), "UPI"));
        assertEquals(new BigDecimal("100.00"), receipt.penaltyCharged());
        assertEquals(new BigDecimal("1100.00"), receipt.totalCharged());
        assertEquals("PAID", receipt.policyStatus());
        assertNotNull(paySvc.getPaymentOverview(rahul, "POL-HD-2026-1002"));
    }

    @Test void deterministicRecommendationFallback() {
        when(policyRepo.findByCustomerCustomerIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(productRepo.findByActiveTrueOrderByProductIdAsc()).thenReturn(List.of(healthProd));
        when(recRepo.save(any())).thenAnswer(inv -> { AiRecommendationRequest r = inv.getArgument(0); r.setRecommendationRequestId(99L); return r; });
        RecommendationService svc = new RecommendationService(productRepo, policyRepo, recRepo, aiProvider, new ObjectMapper());
        RecommendationResponse res = svc.recommend(rahul, new RecommendationRequest(LocalDate.of(1992, 5, 14), "Need health hospital cover", 12, "HEALTH"));
        assertEquals("DETERMINISTIC_ENGINE", res.aiSource());
        assertEquals(1, res.recommendations().size());
    }
}