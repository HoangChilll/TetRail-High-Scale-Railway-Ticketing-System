package com.hoang.ddd.domain.repository;

import com.hoang.ddd.domain.model.entity.Payment;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository {

    Optional<Payment> findById(Long id);

    Optional<Payment> findByPaymentCode(String paymentCode);

    List<Payment> findByBookingId(Long bookingId);

    Optional<Payment> findByPaymentProviderAndProviderTransactionId(String paymentProvider, String providerTransactionId);

    boolean existsByPaymentCode(String paymentCode);

    Payment save(Payment payment);
}
