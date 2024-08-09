package com.reservation.ticket.application.usecase;

import com.reservation.ticket.domain.dto.command.QueueCommand;
import com.reservation.ticket.domain.dto.info.ReservationInfo;
import com.reservation.ticket.domain.entity.concert.reservation.Reservation;
import com.reservation.ticket.domain.entity.concert.reservation.ReservationService;
import com.reservation.ticket.domain.entity.concert.reservation.payment.PaymentService;
import com.reservation.ticket.domain.entity.point.PointService;
import com.reservation.ticket.domain.entity.queue.QueueRedisService;
import com.reservation.ticket.domain.entity.queue.QueueService;
import com.reservation.ticket.domain.entity.userAccount.UserAccountService;
import com.reservation.ticket.domain.enums.PaymentStatus;
import com.reservation.ticket.domain.enums.QueueStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PaymentUsecaseTest {

    @Autowired PaymentUsecase sut;

    @Autowired
    UserAccountService userAccountService;
    @Autowired
    QueueRedisService queueRedisService;
    @Autowired
    ReservationService reservationService;
    @Autowired
    PaymentService paymentService;
    @Autowired
    PointService pointService;

    /**
     *  결제를 진행하여 예약을 완료한다.
     *    과정
     *    - 사용자 id를 이용하여 사용자 검색 (대기열 토큰 조회를 위한)
     *    - 대기열을 조회하여 상태값을 `EXPIRED`로 변경
     *    - 예약 결제상태를 `PAID` 로 변경
     *    - 사용자의 포인트로 예약이 가지고 있는 예약금액을 차감
     *       - 포인트 히스토리 데이터 생성
     *    - 결제 데이터 생성
     *
     *  PS. 결재 데이터 생성, 포인트 히스토리 데이터 생성 확인은 단위테스트에서 확인필요
     */
    @DisplayName("예약된 콘서트를 결제한다.")
    @Test
    public void givenReservationIdAndUserId_whenMakingPayment_thenReservationDone() {
        // given
        Long reservationId = 1L;
        Long userId = 1L;
        String token = "734488355d85";

        // when
        sut.makePayment(reservationId, token);

        // then
        // 대기열의 상태값이 `ACTIVE` -> `EXPIRED` 변경확인
        queueRedisService.expire(token);
        QueueCommand.Get queue = queueRedisService.getQueueByToken(token);
        assertThat(queue).isNull();

        // 예약 상태값 `NOT_PAID` -> `PAID` 변경확인
        Reservation reservation = reservationService.getReservation(reservationId);
        assertThat(reservation.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);

        // 결재 후 사용자의 포인트가 차감 됬는지 확인
        int pointAfterPaid = userAccountService.getUserAccountById(userId).getPoint();
        int expectedUserPoint = 10000;
        int restPoint = expectedUserPoint - reservation.getPrice();
        assertThat(pointAfterPaid).isEqualTo(restPoint);
    }

}