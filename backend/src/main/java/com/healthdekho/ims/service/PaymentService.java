package com.healthdekho.ims.service;
import com.healthdekho.ims.dto.ApiDtos.*;
import com.healthdekho.ims.entity.*;
import com.healthdekho.ims.exception.GlobalExceptionHandler.ApiException;
import com.healthdekho.ims.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service @RequiredArgsConstructor
public class PaymentService {
    private static final BigDecimal PENALTY_RATE = new BigDecimal("0.10");
    private final CustomerPolicyRepository policyRepository;
    private final PremiumInstallmentRepository installmentRepository;
    private final PremiumPaymentRepository paymentRepository;
    private final PolicyService policyService;

    public PolicyPaymentOverviewResponse getPaymentOverview(Customer customer, String policyNumber) {
        CustomerPolicy policy = findOwnedPolicy(customer, policyNumber);
        LocalDate today = LocalDate.now();
        List<InstallmentResponse> installments = installmentRepository.findByPolicyPolicyIdOrderByInstallmentNumberAsc(policy.getPolicyId())
                .stream().map(inst -> toInstallmentResponse(inst, today)).toList();
        List<PaymentLedgerResponse> history = paymentRepository.findByPolicyPolicyIdOrderByCreatedAtDesc(policy.getPolicyId())
                .stream().map(pm -> new PaymentLedgerResponse(pm.getPaymentId(), pm.getTransactionReference(),
                        pm.getInstallment().getInstallmentNumber(), pm.getPaymentDate(), pm.getAmountPaid(),
                        pm.getPenaltyAmount(), pm.getTotalCharged(), pm.getPaymentMode(), pm.getPaymentStatus())).toList();
        BigDecimal totalPaid = installments.stream().map(InstallmentResponse::paidAmount).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal remaining = policy.getTotalPremiumPayable().subtract(totalPaid).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        return new PolicyPaymentOverviewResponse(policyService.toSummary(policy), policy.getTotalPremiumPayable(), totalPaid, remaining, installments, history);
    }

    @Transactional
    public PaymentReceiptResponse processPayment(Customer customer, ProcessPaymentRequest req) {
        CustomerPolicy policy = findOwnedPolicy(customer, req.policyNumber());
        PremiumInstallment inst = installmentRepository.findByIdForUpdate(req.installmentId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Installment not found"));
        if (!inst.getPolicy().getPolicyId().equals(policy.getPolicyId())) throw new ApiException(HttpStatus.BAD_REQUEST, "Installment does not belong to policy");
        if ("PAID".equalsIgnoreCase(inst.getInstallmentStatus())) throw new ApiException(HttpStatus.CONFLICT, "Installment #" + inst.getInstallmentNumber() + " is already paid");

        BigDecimal remainingPrincipal = inst.getScheduledAmount().subtract(inst.getPaidAmount()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal payAmount = req.amount().setScale(2, RoundingMode.HALF_UP);
        if (payAmount.compareTo(remainingPrincipal) > 0) throw new ApiException(HttpStatus.BAD_REQUEST, "Payment exceeds remaining installment balance of " + remainingPrincipal);

        LocalDate today = LocalDate.now();
        boolean pastGrace = today.isAfter(inst.getGracePeriodEndDate());
        BigDecimal penaltyToCharge = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (pastGrace && inst.getPenaltyAmount().compareTo(BigDecimal.ZERO) == 0) {
            penaltyToCharge = inst.getScheduledAmount().multiply(PENALTY_RATE).setScale(2, RoundingMode.HALF_UP);
            inst.setPenaltyAmount(penaltyToCharge);
        }

        BigDecimal newPaid = inst.getPaidAmount().add(payAmount).setScale(2, RoundingMode.HALF_UP);
        inst.setPaidAmount(newPaid);
        if (newPaid.compareTo(inst.getScheduledAmount()) >= 0) {
            inst.setInstallmentStatus("PAID");
            inst.setPaidAt(LocalDateTime.now());
        } else {
            inst.setInstallmentStatus("PARTIALLY_PAID");
        }
        installmentRepository.save(inst);

        BigDecimal totalCharged = payAmount.add(penaltyToCharge).setScale(2, RoundingMode.HALF_UP);
        String txnRef = "TXN-HD-" + ThreadLocalRandom.current().nextInt(100000, 999999);
        paymentRepository.save(PremiumPayment.builder().transactionReference(txnRef).policy(policy).installment(inst).customer(customer)
                .paymentDate(today).amountPaid(payAmount).penaltyAmount(penaltyToCharge).totalCharged(totalCharged)
                .paymentMode(req.paymentMode().trim().toUpperCase()).paymentStatus("SUCCESS").build());

        List<PremiumInstallment> allInst = installmentRepository.findByPolicyPolicyIdOrderByInstallmentNumberAsc(policy.getPolicyId());
        if (allInst.stream().allMatch(i -> "PAID".equalsIgnoreCase(i.getInstallmentStatus()))) {
            policy.setPolicyStatus("PAID");
            policyRepository.save(policy);
        }
        BigDecimal totalPolicyPaid = allInst.stream().map(PremiumInstallment::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal remainingPolicy = policy.getTotalPremiumPayable().subtract(totalPolicyPaid).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal remainingInst = inst.getScheduledAmount().subtract(inst.getPaidAmount()).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        return new PaymentReceiptResponse(txnRef, policy.getPolicyNumber(), policy.getPolicyStatus(), inst.getInstallmentNumber(), inst.getInstallmentStatus(), payAmount, penaltyToCharge, totalCharged, remainingInst, remainingPolicy, today, req.paymentMode().trim().toUpperCase());
    }

    private CustomerPolicy findOwnedPolicy(Customer customer, String policyNumber) {
        CustomerPolicy policy = policyRepository.findByPolicyNumberIgnoreCase(policyNumber.trim())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Policy not found: " + policyNumber));
        if (!policy.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not authorized to access this policy");
        }
        return policy;
    }

    private InstallmentResponse toInstallmentResponse(PremiumInstallment inst, LocalDate today) {
        BigDecimal remPrincipal = inst.getScheduledAmount().subtract(inst.getPaidAmount()).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        boolean isPaid = "PAID".equalsIgnoreCase(inst.getInstallmentStatus());
        boolean pastGrace = !isPaid && today.isAfter(inst.getGracePeriodEndDate());
        BigDecimal penaltyToday = (pastGrace && inst.getPenaltyAmount().compareTo(BigDecimal.ZERO) == 0)
                ? inst.getScheduledAmount().multiply(PENALTY_RATE).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        String displayStatus = isPaid ? "PAID" : (pastGrace && "PENDING".equalsIgnoreCase(inst.getInstallmentStatus()) ? "OVERDUE" : inst.getInstallmentStatus());
        return new InstallmentResponse(inst.getInstallmentId(), inst.getInstallmentNumber(), inst.getDueDate(), inst.getGracePeriodEndDate(),
                inst.getScheduledAmount(), inst.getPaidAmount(), inst.getPenaltyAmount(), remPrincipal, penaltyToday,
                remPrincipal.add(penaltyToday).setScale(2, RoundingMode.HALF_UP), displayStatus, inst.getPaidAt());
    }
}
