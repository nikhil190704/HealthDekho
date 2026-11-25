package com.healthdekho.ims.controller;
import com.healthdekho.ims.dto.ApiDtos.*;
import com.healthdekho.ims.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/payments") @RequiredArgsConstructor
public class PaymentController {
    private final AuthService authService;
    private final PaymentService paymentService;

    @GetMapping("/policies/{policyNumber}")
    public PolicyPaymentOverviewResponse overview(Authentication auth, @PathVariable String policyNumber) {
        return paymentService.getPaymentOverview(authService.getCustomerByEmail(auth.getName()), policyNumber);
    }

    @PostMapping("/process")
    public PaymentReceiptResponse process(Authentication auth, @Valid @RequestBody ProcessPaymentRequest req) {
        return paymentService.processPayment(authService.getCustomerByEmail(auth.getName()), req);
    }
}
