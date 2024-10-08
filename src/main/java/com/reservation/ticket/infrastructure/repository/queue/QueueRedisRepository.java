package com.reservation.ticket.infrastructure.repository.queue;

import com.reservation.ticket.domain.entity.queue.QueueRepository;
import com.reservation.ticket.domain.enums.QueueStatus;
import com.reservation.ticket.infrastructure.dto.entity.QueueEntity;
import com.reservation.ticket.infrastructure.dto.statement.QueueStatement;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class QueueRedisRepository implements QueueRepository {

    private final RedisTemplate<String, String> redisTemplate;
    private ZSetOperations<String, String> zSetOps;

    @PostConstruct
    public void initializingOperations() {
        this.zSetOps = redisTemplate.opsForZSet();
    }

    /**
     * QueueStatus 에 따라서 대기열을 생성해주면 될거 같다.
     */
    public QueueEntity save(QueueStatement statement) {
        this.zSetOps.add(statement.queueStatus().name(), statement.token(), System.currentTimeMillis());
        return QueueEntity.of(statement.token());
    }

    @Override
    public QueueEntity getQueueByUserId(Long userId) {
        return null;
    }

    @Override
    public List<QueueEntity> getQueuesByStatus(QueueStatus queueStatus) {
        return List.of();
    }

    public List<QueueEntity> getQueuesByStatusPerLimit(QueueStatus queueStatus, int limit) {
        Set<String> values = this.zSetOps.range(queueStatus.name(), 0, limit);
        assert values != null;
        return values.stream().map(QueueEntity::of).toList();
    }

    public QueueEntity getQueueByToken(QueueStatement queueStatement) {
        Long rank = this.zSetOps.rank(queueStatement.queueStatus().name(), queueStatement.token());
        if (rank == null) {
            return null;
        }
        return QueueEntity.of(queueStatement.token());
    }

    @Override
    public int countByStatus(QueueStatus status) {
        return 0;
    }

    public void removeQueue(QueueStatement statement) {
        this.zSetOps.remove(statement.queueStatus().name(), statement.token());
    }

    @Override
    public void removeQueues(QueueStatus queueStatus, List<String> queues) {
        /**
         * 배열을 넣어주어야 하는건가..
         */
        String[] strings = new String[queues.size()];
        for (int i = 0; i < queues.size(); i++) {
            strings[i] = queues.get(i);
        }
        this.zSetOps.remove(queueStatus.name(), strings);
    }

}
