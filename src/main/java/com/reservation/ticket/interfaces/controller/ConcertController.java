package com.reservation.ticket.interfaces.controller;

import com.reservation.ticket.domain.entity.concert.ConcertService;
import com.reservation.ticket.interfaces.dto.ConcertDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/concerts")
@RequiredArgsConstructor
public class ConcertController {

    private final ConcertService concertService;

    @GetMapping
    public ResponseEntity<List<ConcertDto.Response>> selectConcerts() {
        List<ConcertDto.Response> responses = concertService.selectAllConcerts().stream()
                .map(ConcertDto.Response::from)
                .toList();

        return ResponseEntity.ok().body(responses);
    }

    @GetMapping("/paging")
    public ResponseEntity<List<ConcertDto.Response>> selectConcertsPage(
            @PageableDefault(size = 10) Pageable pageable
    ) {
        List<ConcertDto.Response> list = concertService.selectAllConcertsByPaging(pageable).stream()
                .map(ConcertDto.Response::from)
                .toList();
        return ResponseEntity.ok().body(list);
    }


}

