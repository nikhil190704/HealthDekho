package com.healthdekho.ims.repository;
import com.healthdekho.ims.entity.InsuranceProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface InsuranceProductRepository extends JpaRepository<InsuranceProduct, Long> {
    List<InsuranceProduct> findByActiveTrueOrderByProductIdAsc();
    Optional<InsuranceProduct> findByProductCodeIgnoreCase(String productCode);
}
