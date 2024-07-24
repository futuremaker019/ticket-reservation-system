package com.reservation.ticket.domain.service;

import com.reservation.ticket.domain.entity.complex.ReservationSeat;
import com.reservation.ticket.domain.entity.complex.ReservationSeatComplexIds;
import com.reservation.ticket.domain.repository.ReservationSeatRepository;
import com.reservation.ticket.infrastructure.exception.ApplicationException;
import com.reservation.ticket.infrastructure.exception.ErrorCode;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationSeatService {

    private final ReservationSeatRepository reservationSeatRepository;

    public void save(Long reservationId, Long concertScheduleId, List<Long> seatIds, LockModeType lockModeType) {
        switch (lockModeType) {
            case NONE -> checkIfSeatsAvailable(concertScheduleId, seatIds);
            case PESSIMISTIC_READ -> checkIfSeatsAvailableWithPessimisticLock(concertScheduleId, seatIds);
            case OPTIMISTIC -> checkIfSeatsAvailableWithOptimisticLock(concertScheduleId, seatIds);
        }

        seatIds.forEach(seatId -> {
            ReservationSeat reservationSeat = ReservationSeat.of(
                    new ReservationSeatComplexIds(concertScheduleId, seatId, reservationId));
            reservationSeatRepository.save(reservationSeat);
        });
    }

    public void checkIfSeatsAvailable(Long concertScheduleId, List<Long> seatIds) {
        List<ReservationSeat> reservationSeats = reservationSeatRepository.selectSeatsByScheduleId(concertScheduleId);
        checkSeats(reservationSeats, seatIds);
    }

    public void checkIfSeatsAvailableWithPessimisticLock(Long concertScheduleId, List<Long> seatIds) {
        List<ReservationSeat> reservationSeats = reservationSeatRepository.selectSeatsByScheduleIdWithPessimisticLock(concertScheduleId);
        checkSeats(reservationSeats, seatIds);
    }

    private void checkIfSeatsAvailableWithOptimisticLock(Long concertScheduleId, List<Long> seatIds) {
        List<ReservationSeat> reservationSeats = reservationSeatRepository.selectSeatsByScheduleIdWithOptimisticLock(concertScheduleId);
        checkSeats(reservationSeats, seatIds);
    }

    public void checkSeats(List<ReservationSeat> reservationSeats, List<Long> seatIds) {
        List<Long> reservedSeatIds = reservationSeats.stream().map(reservationSeat -> reservationSeat.getId().getSeatId()).toList();
        ArrayList<Long> copiedSeatIds = new ArrayList<>(seatIds);
        copiedSeatIds.retainAll(reservedSeatIds);       // 이미 예약된 좌석이면 예외처리 한다.
        if (!copiedSeatIds.isEmpty()) {
            throw new ApplicationException(ErrorCode.SEAT_ALREADY_OCCUPIED, "seat already occupied : %s".formatted(copiedSeatIds));
        }
    }

    public void releaseSeats(List<Long> reservationIds) {
        reservationSeatRepository.removeSeats(reservationIds);
    }
}
