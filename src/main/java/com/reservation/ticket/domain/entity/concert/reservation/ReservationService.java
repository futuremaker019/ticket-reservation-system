package com.reservation.ticket.domain.entity.concert.reservation;

import com.reservation.ticket.domain.dto.command.ReservationCommand;
import com.reservation.ticket.domain.dto.info.ReservationInfo;
import com.reservation.ticket.domain.entity.userAccount.UserAccount;
import com.reservation.ticket.domain.entity.concert.reservation.ticket.Ticket;
import com.reservation.ticket.domain.entity.concert.reservation.ticket.TicketComplexIds;
import com.reservation.ticket.domain.enums.LockType;
import com.reservation.ticket.domain.enums.PaymentStatus;
import com.reservation.ticket.domain.enums.ReservationStatus;
import com.reservation.ticket.domain.entity.concert.reservation.ticket.TicketRepository;
import com.reservation.ticket.domain.exception.ApplicationException;
import com.reservation.ticket.domain.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final TicketRepository ticketRepository;
    private final RedissonClient redissonClient;

    /**
     *  예약 id를 이용하여 예약 정보를 가져온다.
     */
    public ReservationInfo getReservationById(Long reservationId) {
        return ReservationInfo.from(reservationRepository.findById(reservationId));
    }

    @Transactional
    public ReservationInfo reserve(ReservationCommand.Create create, UserAccount userAccount, LockType lockType) {
        Reservation reservation = Reservation.of(userAccount, create.price());
        Reservation savedReservation = reservationRepository.reserve(reservation);

        // 좌석의 점유상태를 검증한다.
        switch (lockType) {
            case NONE -> checkIfSeatsAvailable(create.concertScheduleId(), create.seatIds());
            case PESSIMISTIC_READ -> checkIfSeatsAvailableWithPessimisticLock(create.concertScheduleId(), create.seatIds());
            case DISTRIBUTED_LOCK -> checkIfSeatsAvailableWithDistributedLock(create.concertScheduleId(), create.seatIds());
        }

        occupySeats(create, savedReservation);
        return ReservationInfo.from(savedReservation);
    }

    /**
     * 좌석을 점유한다.
     */
    public void occupySeats(ReservationCommand.Create create, Reservation savedReservation) {
        // 예약시 선택한 자리를 점유한다.
        create.seatIds().forEach(seatId -> {
            Ticket ticket = Ticket.of(
                    new TicketComplexIds(create.concertScheduleId(), seatId, savedReservation.getId()));
            ticketRepository.issue(ticket);
        });
    }

    public List<Reservation> selectReservationsByReservationStatus(ReservationStatus reservationStatus) {
        return reservationRepository.findAllByReservationStatus(reservationStatus);
    }

    @Transactional
    public Reservation getReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId);
        reservation.changePaymentStatus(PaymentStatus.PAID);
        return reservation;
    }

    @Transactional
    public void changePaymentStatus(Long reservationId, PaymentStatus paymentStatus) {
        Reservation reservation = reservationRepository.findById(reservationId);
        reservation.changePaymentStatus(paymentStatus);
    }

    public void checkIfSeatsAvailable(Long concertScheduleId, List<Long> seatIds) {
        List<Ticket> tickets = ticketRepository.getSeats(concertScheduleId, seatIds);
        checkSeats(tickets);
    }

    public void checkIfSeatsAvailableWithPessimisticLock(Long concertScheduleId, List<Long> seatIds) {
        List<Ticket> tickets = ticketRepository.getSeatsWithPessimisticLock(concertScheduleId, seatIds);
        checkSeats(tickets);
    }

    public void checkSeats(List<Ticket> tickets) {
        if (!tickets.isEmpty()) {
            throw new ApplicationException(ErrorCode.SEAT_ALREADY_OCCUPIED, "seat already occupied : %s".formatted(tickets));
        }
    }

    private void checkIfSeatsAvailableWithDistributedLock(Long concertScheduleId, List<Long> seatIds) {
        RLock lock = redissonClient.getLock("reservation");
        try {
            boolean available = lock.tryLock(10, 1, TimeUnit.SECONDS);
            if (!available) {
                throw new RuntimeException("Lock을 획득하지 못했습니다.");
            }
            log.info("locked : {}", available);
            List<Ticket> tickets = ticketRepository.getSeats(concertScheduleId, seatIds);
            if (!tickets.isEmpty()) {
                throw new ApplicationException(ErrorCode.SEAT_ALREADY_OCCUPIED, "seat already occupied : %s".formatted(tickets));
            }
        } catch(InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 1. 예약된 상위 10개의 목록을 조회하여 결재상태가 NOT_PAID 이며, 현재시간보다 5분 초과된 상태면
     * `ACTIVE`(예약중) 인 상태를 `CANCELLED`(취소) 로 변경한다.
     * 2. 예약으로 선점된 좌석을 다시 원상복구 한다.
     */
    @Transactional
    public void cancelReservation() {
        int limit = 10;
        List<Long> cancelledIds = new ArrayList<>();
        List<Reservation> reservations =
                reservationRepository.findAllByReservationStatusOrderByIdAsc(ReservationStatus.ACTIVE, limit);
        reservations.forEach(reservation -> {
            if (reservation.getPaymentStatus() == PaymentStatus.NOT_PAID
                    && reservation.getReservedAt().plusMinutes(5).isBefore(LocalDateTime.now())) {
                reservation.changeReservationStatus(ReservationStatus.CANCELLED);
                cancelledIds.add(reservation.getId());
            }
        });
        releaseSeats(cancelledIds);
    }

    public void releaseSeats(List<Long> reservationIds) {
        ticketRepository.removeSeats(reservationIds);
    }
}

