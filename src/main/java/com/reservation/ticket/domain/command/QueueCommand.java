package com.reservation.ticket.domain.command;

import com.reservation.ticket.domain.entity.Queue;
import com.reservation.ticket.domain.enums.QueueStatus;

import java.time.LocalDateTime;

public class QueueCommand {
    public record Get(Long id,
                      UserCommand.Get user,
                      String token,
                      QueueStatus status,
                      LocalDateTime shouldExpiredAt,
                      LocalDateTime createdAt
    ) {
        public static Get of(Long id, UserCommand.Get user, String token, QueueStatus status, LocalDateTime shouldExpiredAt, LocalDateTime createdAt) {
            return new Get(id, user, token, status, shouldExpiredAt, createdAt);
        }

        public static Get of(Long id, String token, QueueStatus status, LocalDateTime shouldExpiredAt, LocalDateTime createdAt) {
            return new Get(id, null, token, status, shouldExpiredAt, createdAt);
        }

        public static Get from(Queue queue) {
            return Get.of(
                    queue.getId(),
                    queue.getToken(),
                    queue.getQueueStatus(),
                    queue.getShouldExpiredAt(),
                    queue.getCreatedAt()
            );
        }
    }
}
