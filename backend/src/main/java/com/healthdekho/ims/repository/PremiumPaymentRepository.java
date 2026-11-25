package com.healthdekho.ims.repository;
import com.healthdekho.ims.entity.PremiumPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PremiumPaymentRepository extends JpaRepository<PremiumPayment, Long> {
    List<PremiumPayment> findByPolicyPolicyIdOrderByCreatedAtDesc(Long policyId);
}
