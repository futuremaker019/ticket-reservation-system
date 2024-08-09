package com.reservation.ticket.application.usecase;

import com.reservation.ticket.domain.common.DataPlatformClient;
import com.reservation.ticket.domain.entity.concert.reservation.Reservation;
import com.reservation.ticket.domain.entity.concert.reservation.ReservationService;
import com.reservation.ticket.domain.entity.concert.reservation.payment.Payment;
import com.reservation.ticket.domain.entity.concert.reservation.payment.PaymentService;
import com.reservation.ticket.domain.entity.point.event.PointEvent;
import com.reservation.ticket.domain.entity.queue.QueueRedisService;
import com.reservation.ticket.domain.entity.userAccount.UserAccount;
import com.reservation.ticket.domain.entity.userAccount.UserAccountService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class PaymentUsecase {

    private static final Logger log = LoggerFactory.getLogger(PaymentUsecase.class);
    private final UserAccountService userAccountService;
    private final QueueRedisService queueRedisService;
    private final ReservationService reservationService;
    private final PaymentService paymentService;

    public final ApplicationEventPublisher publisher;

    private final DataPlatformClient dataPlatformClient;

    public void makePayment(Long reservationId, String token) {
        UserAccount userAccount = userAccountService.getUserAccountByToken(token);
        Reservation reservation = reservationService.getReservation(reservationId);
        // 예약완료를 위한 결제정보 등록
        Payment payment = paymentService.createPayment(reservation, userAccount);
        // 대기열 토큰 검증 및 만료 - redis에 저장된 ACTIVE 토큰 삭제
        queueRedisService.expire(token);

        /**
         * 포인트 차감 이벤트
         *  - 포인트가 예약금액 보다 적다면 예외를 발생시킴
         */
        publisher.publishEvent(
                PointEvent.Use.of(
                        userAccount.getId(), userAccount.getPoint(), reservation.getPrice(),
                        reservation.getId(), payment.getId()
                )
        );

        // 데이터 플랫폼으로 예약정보 전송
        callDataPlatform(reservation.getId());
    }

    @Retryable(
        maxAttempts = 2,                    // 최대 시도 횟수
        backoff = @Backoff(delay = 1000)    // 재시도 간격
    )
    public void callDataPlatform(Long reservationId) {
        boolean result = dataPlatformClient.send(reservationId);
        if (!result) {
            log.warn("data platform send failed");
        }
    }

}
