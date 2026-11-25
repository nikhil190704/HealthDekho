package com.healthdekho.ims.controller;
import com.healthdekho.ims.dto.ApiDtos.ProductResponse;
import com.healthdekho.ims.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/v1/products") @RequiredArgsConstructor
public class ProductController {
    private final AuthService authService;
    private final ProductService productService;

    @GetMapping("/available")
    public List<ProductResponse> available(Authentication auth) {
        return productService.getAvailableProducts(authService.getCustomerByEmail(auth.getName()));
    }
}
