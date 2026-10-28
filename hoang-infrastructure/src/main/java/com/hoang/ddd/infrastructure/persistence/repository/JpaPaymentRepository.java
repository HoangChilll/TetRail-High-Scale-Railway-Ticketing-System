package com.hoang.ddd.infrastructure.persistence.repository;

import com.hoang.ddd.domain.model.entity.Payment;
import com.hoang.ddd.domain.repository.PaymentRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaPaymentRepository extends JpaRepository<Payment, Long>, PaymentRepository {

    @Override
    Optional<Payment> findByPaymentCode(String paymentCode);

    @Override
    List<Payment> findByBookingId(Long bookingId);

    @Override
    Optional<Payment> findByPaymentProviderAndProviderTransactionId(String paymentProvider, String providerTransactionId);

    @Override
    boolean existsByPaymentCode(String paymentCode);
}
