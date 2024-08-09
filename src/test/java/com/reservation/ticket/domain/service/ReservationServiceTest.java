package com.reservation.ticket.domain.service;

import com.reservation.ticket.domain.dto.command.ReservationCommand;
import com.reservation.ticket.domain.dto.info.ReservationInfo;
import com.reservation.ticket.domain.entity.concert.reservation.Reservation;
import com.reservation.ticket.domain.entity.userAccount.UserAccount;
import com.reservation.ticket.domain.entity.concert.reservation.ReservationService;
import com.reservation.ticket.domain.enums.LockType;
import com.reservation.ticket.domain.enums.PaymentStatus;
import com.reservation.ticket.domain.enums.ReservationStatus;
import com.reservation.ticket.domain.entity.concert.reservation.ReservationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @InjectMocks
    ReservationService sut;

    @Mock ReservationRepository reservationRepository;

    /**
     * 사용자 id, 예약금액으로 예약을 저장한다.
     *    - 결제 상태와 예약 상태는 각각 `NOT_PAID(미결제)`, `ACTIVE(예약중)` 상태로 저장됨 (Default)
     */
    @DisplayName("예약금액  으로 예약을 저장한다.")
    @Test
    void givenUserIdAndPrice_whenRequestingSaveReservation_thenSavesReservation() {
        // given
        Long userId = 1L;
        UserAccount userAccount = UserAccount.of(userId);

        int price = 1000;
        Long concertScheduleId = 1L;
        List<Long> seatIds = List.of(1L, 2L);
        ReservationCommand.Create create = ReservationCommand.Create.of(concertScheduleId, seatIds, price);

        Long reservationId = 1L;
        Reservation reservation = Reservation.of(userAccount, price, PaymentStatus.NOT_PAID, ReservationStatus.ACTIVE);
        given(reservationRepository.reserve(any(Reservation.class))).willReturn(reservation);

        // when
        ReservationInfo savedReservation = sut.reserve(create, userAccount, LockType.NONE);

        // then
        assertThat(savedReservation.price()).isEqualTo(reservation.getPrice());
        assertThat(savedReservation.paymentStatus()).isEqualTo(reservation.getPaymentStatus());
        assertThat(savedReservation.reservationStatus()).isEqualTo(reservation.getReservationStatus());

        ArgumentCaptor<Reservation> reservationArgumentCaptor = ArgumentCaptor.forClass(Reservation.class);
        then(reservationRepository).should().reserve(reservationArgumentCaptor.capture());

        Reservation argumentCaptorValue = reservationArgumentCaptor.getValue();
        assertThat(argumentCaptorValue).isNotNull();
        assertThat(argumentCaptorValue.getPrice()).isEqualTo(price);
        assertThat(argumentCaptorValue.getUserAccount()).isEqualTo(userAccount);
    }

    @DisplayName("에약 id로 예약을 조회하여 결재상태를 `PAID`로 변경한다.")
    @Test
    public void givenReservationId_whenChangingPaymentStatusAsPaid_thenChangingPaymentStatusAsPaid() {
        // given
        Long reservationId = 1L;
        Long userId = 1L;
        Reservation reservation = Reservation.of(reservationId, UserAccount.of(userId), 1000);
        given(reservationRepository.findById(reservationId)).willReturn(reservation);

        // when
        Reservation changedReservation = sut.getReservation(reservationId);

        // then
        assertThat(changedReservation.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);

        then(reservationRepository).should().findById(reservationId);
    }

}