package com.healthdekho.ims.controller;
import com.healthdekho.ims.dto.ApiDtos.*;
import com.healthdekho.ims.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/auth") @RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) { return authService.login(req); }

    @GetMapping("/me")
    public CustomerProfile me(Authentication auth) { return authService.toProfile(authService.getCustomerByEmail(auth.getName())); }
}
