package com.reservation.ticket.infrastructure.repository.payment;

import com.reservation.ticket.domain.entity.concert.reservation.payment.Payment;
import com.reservation.ticket.domain.entity.concert.reservation.payment.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {

    private final PaymentJpaRepository paymentJpaRepository;

    @Override
    public Payment save(Payment payment) {
        return paymentJpaRepository.save(payment);
    }

    @Override
    public void delete(Long paymentId) {
        paymentJpaRepository.deleteById(paymentId);
    }

}
