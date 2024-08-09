package com.reservation.ticket.domain.entity.queue;

import com.reservation.ticket.domain.dto.command.QueueCommand;
import com.reservation.ticket.domain.entity.userAccount.UserAccount;
import com.reservation.ticket.domain.entity.userAccount.UserAccountRepository;
import com.reservation.ticket.domain.enums.QueueStatus;
import com.reservation.ticket.domain.exception.ApplicationException;
import com.reservation.ticket.domain.exception.ErrorCode;
import com.reservation.ticket.infrastructure.dto.entity.QueueEntity;
import com.reservation.ticket.infrastructure.dto.statement.QueueStatement;
import com.reservation.ticket.infrastructure.repository.queue.QueueRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QueueRedisService {

    private final UserAccountRepository userAccountRepository;
    private final QueueRedisRepository queueRedisRepository;

    public String createWaitQueue(Long userId) {
        UserAccount userAccount = userAccountRepository.findById(userId);
        String token = generateToken();
        // 생성된 토큰을 사용자 정보에 저장
        userAccount.saveToken(token);
        queueRedisRepository.save(QueueStatement.of(userAccount, token, QueueStatus.WAIT));
        // response header에 넣어주기 위한 토큰을 리턴
        return token;
    }

    public void recoverQueue(Long userId, QueueStatus queueStatus) {
        UserAccount userAccount = userAccountRepository.findById(userId);
        queueRedisRepository.save(QueueStatement.of(userAccount, userAccount.getToken(), queueStatus));
    }

    public List<QueueCommand.Get> selectQueueByStatus(QueueStatus status) {
        return List.of();
    }

    public void renewQueueExpirationDate(String token) {

    }

    public void removeActiveQueue(String token) {
        queueRedisRepository.removeQueue(QueueStatement.of(token, QueueStatus.ACTIVE));
    }

    public void removeWaitQueue(String token) {

    }

    public QueueCommand.Get getQueueByToken(String token) {
        return null;
    }

    public void verifyQueue(String token) {
        QueueEntity queue = queueRedisRepository.getQueueByToken(QueueStatement.of(token, QueueStatus.ACTIVE));
        if (queue == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "token is not valid : %s".formatted(token));
        }
    }

    public void changeTokenStatusToExpire() {

    }

    public void changeTokenStatusToActive() {
        int limit = 30;
        List<QueueEntity> queues = queueRedisRepository.getQueuesByStatusPerLimit(QueueStatus.WAIT, limit);
        for (QueueEntity queue : queues) {
            queueRedisRepository.save(QueueStatement.of(queue.getToken(), QueueStatus.ACTIVE));
        }
    }

    public void expire(String token) {
        queueRedisRepository.removeQueue(QueueStatement.of(token, QueueStatus.ACTIVE));
    }

    private String generateToken() {
        String uuid = UUID.randomUUID().toString();
        return uuid.substring(uuid.lastIndexOf("-") + 1);
    }
}
