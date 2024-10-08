package com.reservation.ticket.interfaces.dto;

import com.reservation.ticket.domain.dto.command.ConcertCommand;

public class ConcertDto {
    public record Response(Long id, String name) {
        public static Response of(Long id, String name) {
            return new Response(id, name);
        }

        public static Response from(ConcertCommand.Get commandGet) {
            return Response.of(commandGet.id(), commandGet.name());
        }
    }

    public record Request() {

    }
}
