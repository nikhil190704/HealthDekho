package com.healthdekho.ims.controller;
import com.healthdekho.ims.dto.ApiDtos.*;
import com.healthdekho.ims.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/recommendations") @RequiredArgsConstructor
public class RecommendationController {
    private final AuthService authService;
    private final RecommendationService recommendationService;

    @PostMapping
    public RecommendationResponse recommend(Authentication auth, @Valid @RequestBody RecommendationRequest req) {
        return recommendationService.recommend(authService.getCustomerByEmail(auth.getName()), req);
    }
}
