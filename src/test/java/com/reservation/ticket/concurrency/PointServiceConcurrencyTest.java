package com.reservation.ticket.concurrency;

import com.reservation.ticket.domain.entity.userAccount.UserAccountRepository;
import com.reservation.ticket.domain.entity.point.PointService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class PointServiceConcurrencyTest {

    @Autowired
    PointService pointService;
    @Autowired
    UserAccountRepository userAccountRepository;

    @DisplayName("포인트를 연속 15번 충전 동시성 이슈 테스트")
    @Test
    public void chargingPointOptimisticLockTest() throws InterruptedException {
        // given
        int chargeablePoint = 100;
        String token = "6f8f504681f9";

        /**
         * 5번 포인트를 연속으로 충전시 최초의 한번의 포인트 충전 요청만 적용되도록 한다.
         */
        int threadCount = 15;
        AtomicInteger counter = new AtomicInteger(0);
        ExecutorService executorService = Executors.newFixedThreadPool(30);
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.chargePoint(chargeablePoint, token);
                } catch (Exception e) {
//                    e.printStackTrace();
                    counter.incrementAndGet();
                } finally {
                    countDownLatch.countDown();
                }
            });
        }
        countDownLatch.await();
        executorService.shutdown();

        assertThat(counter.get()).isEqualTo(threadCount - 1);
    }

}
