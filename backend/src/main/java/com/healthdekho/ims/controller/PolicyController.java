package com.healthdekho.ims.controller;
import com.healthdekho.ims.dto.ApiDtos.*;
import com.healthdekho.ims.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController @RequestMapping("/api/v1/policies") @RequiredArgsConstructor
public class PolicyController {
    private final AuthService authService;
    private final PolicyService policyService;

    @GetMapping("/my")
    public List<PolicySummaryResponse> myPolicies(Authentication auth) {
        return policyService.getMyPolicies(authService.getCustomerByEmail(auth.getName()));
    }

    @GetMapping("/search")
    public PagedResponse<PolicySummaryResponse> search(Authentication auth,
            @RequestParam(required = false) String policyNumber,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOfBirth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return policyService.searchMyPolicies(authService.getCustomerByEmail(auth.getName()), policyNumber, customerName, dateOfBirth, page, size);
    }

    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    public PolicySummaryResponse register(Authentication auth, @Valid @RequestBody RegisterPolicyRequest req) {
        return policyService.registerPolicy(authService.getCustomerByEmail(auth.getName()), req);
    }
}
