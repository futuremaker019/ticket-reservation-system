package com.reservation.ticket.domain.entity.concert.reservation.payment;

public interface PaymentRepository {

    Payment save(Payment payment);

    void delete(Long paymentId);
}
