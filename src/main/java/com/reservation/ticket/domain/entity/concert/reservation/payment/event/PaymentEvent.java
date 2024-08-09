package com.reservation.ticket.domain.entity.concert.reservation.payment.event;

public class PaymentEvent {

    public record Recover(Long reservationId, Long paymentId, Long userId) {
        public static Recover of(Long reservationId, Long paymentId, Long userId) {
            return new Recover(reservationId, paymentId, userId);
        }
    }

}
