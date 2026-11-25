package com.healthdekho.ims.repository;
import com.healthdekho.ims.entity.PremiumInstallment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface PremiumInstallmentRepository extends JpaRepository<PremiumInstallment, Long> {
    List<PremiumInstallment> findByPolicyPolicyIdOrderByInstallmentNumberAsc(Long policyId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM PremiumInstallment i WHERE i.installmentId = :id")
    Optional<PremiumInstallment> findByIdForUpdate(@Param("id") Long id);
}
