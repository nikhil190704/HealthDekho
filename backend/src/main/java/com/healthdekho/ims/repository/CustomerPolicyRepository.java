package com.healthdekho.ims.repository;
import com.healthdekho.ims.entity.CustomerPolicy;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.*;
public interface CustomerPolicyRepository extends JpaRepository<CustomerPolicy, Long> {
    List<CustomerPolicy> findByCustomerCustomerIdOrderByCreatedAtDesc(Long customerId);
    Optional<CustomerPolicy> findByPolicyNumberIgnoreCase(String policyNumber);
    boolean existsByCustomerCustomerIdAndProductProductId(Long customerId, Long productId);
    @Query("SELECT cp FROM CustomerPolicy cp JOIN cp.customer c JOIN cp.product pr WHERE c.customerId = :customerId AND (:policyNumber IS NULL OR LOWER(cp.policyNumber) LIKE LOWER(CONCAT('%', :policyNumber, '%'))) AND (:customerName IS NULL OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :customerName, '%'))) AND (:dob IS NULL OR c.dateOfBirth = :dob) ORDER BY cp.createdAt DESC")
    Page<CustomerPolicy> searchOwnedPolicies(@Param("customerId") Long customerId, @Param("policyNumber") String policyNumber, @Param("customerName") String customerName, @Param("dob") LocalDate dob, Pageable pageable);
}
