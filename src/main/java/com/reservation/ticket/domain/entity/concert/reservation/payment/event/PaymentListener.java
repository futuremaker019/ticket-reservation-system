package com.reservation.ticket.domain.entity.concert.reservation.payment.event;

import com.reservation.ticket.domain.entity.concert.reservation.ReservationService;
import com.reservation.ticket.domain.entity.concert.reservation.payment.PaymentService;
import com.reservation.ticket.domain.entity.queue.QueueRedisService;
import com.reservation.ticket.domain.enums.PaymentStatus;
import com.reservation.ticket.domain.enums.QueueStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentListener {

    private final ReservationService reservationService;
    private final PaymentService paymentService;
    private final QueueRedisService queueRedisService;

    @EventListener
    public void recoverFailure(PaymentEvent.Recover event) {
        reservationService.changePaymentStatus(event.reservationId(), PaymentStatus.NOT_PAID);
        paymentService.deletePayment(event.paymentId());
        queueRedisService.recoverQueue(event.userId(), QueueStatus.ACTIVE);
    }

}
