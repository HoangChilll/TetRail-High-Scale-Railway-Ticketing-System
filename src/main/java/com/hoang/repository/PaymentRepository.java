package com.hoang.repository;

import com.hoang.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentCode(String paymentCode);

    List<Payment> findByBookingId(Long bookingId);

    Optional<Payment> findByPaymentProviderAndProviderTransactionId(String paymentProvider, String providerTransactionId);

    boolean existsByPaymentCode(String paymentCode);
}
