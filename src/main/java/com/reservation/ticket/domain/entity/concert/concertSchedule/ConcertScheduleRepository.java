package com.reservation.ticket.domain.entity.concert.concertSchedule;

import com.reservation.ticket.infrastructure.dto.entity.ConcertScheduleEntity;

import java.time.LocalDateTime;
import java.util.List;

public interface ConcertScheduleRepository {

    List<ConcertSchedule> findAllConcertSchedules();
    List<ConcertSchedule> findAllByConcertId(Long concertId);

    ConcertSchedule findByOpenedAt(LocalDateTime openedAt);
    ConcertSchedule findById(Long id);

    List<ConcertScheduleEntity> getConcertSchedule(Long concertId);

}
