package com.healthdekho.ims.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
@Entity @Table(name = "customer")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Customer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "customer_id") private Long customerId;
    @Column(name = "full_name", nullable = false) private String fullName;
    @Column(nullable = false, unique = true) private String email;
    @Column(name = "password_hash", nullable = false) private String passwordHash;
    @Column(name = "date_of_birth", nullable = false) private LocalDate dateOfBirth;
    @Column(name = "phone_number", nullable = false) private String phoneNumber;
    private String city;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    @PrePersist void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
}