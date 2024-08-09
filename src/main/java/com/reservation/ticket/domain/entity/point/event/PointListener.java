package com.reservation.ticket.domain.entity.point.event;

import com.reservation.ticket.domain.entity.concert.reservation.payment.event.PaymentEvent;
import com.reservation.ticket.domain.entity.point.PointService;
import com.reservation.ticket.domain.entity.userAccount.UserAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PointListener {

    private final PointService pointService;
    private final ApplicationEventPublisher publisher;

    /**
     * 예약금액을 포인트로 차감한다.
     *  포인트가 부족시 보상 트랜잭션을 발행한다.
     *      (publish payment recover event - `MSA`를 위해 event 형태로 처리)
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void usePointEvent(PointEvent.Use event) {
        try {
            pointService.usePoint(
                    event.price(), UserAccount.of(event.userId(), event.userPoint())
            );
        } catch (Exception e) {
            publisher.publishEvent(
                    PaymentEvent.Recover.of(
                            event.reservationId(),
                            event.paymentId(),
                            event.userId()
                    )
            );
            throw e;
        }
    }
}
