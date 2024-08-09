package com.reservation.ticket.domain.entity.point.event;

public class PointEvent {

    public record Use(Long userId, int userPoint, int price, Long reservationId, Long paymentId) {
        public static Use of(Long userId, int userPoint, int price, Long reservationId, Long paymentId) {
            return new Use(userId, userPoint, price, reservationId, paymentId);
        }
    }

}
