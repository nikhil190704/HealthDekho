package com.healthdekho.ims.service;
import com.healthdekho.ims.dto.ApiDtos.*;
import com.healthdekho.ims.entity.Customer;
import com.healthdekho.ims.exception.GlobalExceptionHandler.ApiException;
import com.healthdekho.ims.repository.CustomerRepository;
import com.healthdekho.ims.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.stereotype.Service;
import java.time.*;

@Service @RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authManager;
    private final CustomerRepository customerRepository;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest req) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(req.email().trim(), req.password()));
        Customer c = getCustomerByEmail(req.email().trim());
        String token = jwtService.generateToken(c.getCustomerId(), c.getEmail());
        return new LoginResponse(token, "Bearer", jwtService.getExpirationSeconds(), toProfile(c));
    }

    public Customer getCustomerByEmail(String email) {
        return customerRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Authenticated customer profile not found"));
    }

    public CustomerProfile toProfile(Customer c) {
        int age = Period.between(c.getDateOfBirth(), LocalDate.now()).getYears();
        return new CustomerProfile(c.getCustomerId(), c.getFullName(), c.getEmail(), c.getDateOfBirth(), age, c.getPhoneNumber(), c.getCity());
    }
}