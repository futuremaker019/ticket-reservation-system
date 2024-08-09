package com.reservation.ticket.domain.entity.point.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@RecordApplicationEvents
@SpringBootTest
class PointListenerTest {

    @Autowired
    ApplicationEvents applicationEvents;

    @MockBean
    PointListener pointListener;

    @DisplayName("이벤트를 이용하여 예약금액을 포인트에서 차감한다.")
    @Test
    void pointEventTest() {
        // given
        Long userId = 1L;
        int userPoint = 1000;
        int price = 100;
        Long reservationId = 1L;
        Long paymentId = 1L;
        PointEvent.Use useEvent = PointEvent.Use.of(userId, userPoint, price, reservationId, paymentId);

        // when
        pointListener.usePointEvent(useEvent);

        // then
        assertThat(applicationEvents.stream(PointEvent.Use.class))
                .hasSize(1)
                .anySatisfy(event -> {
                    assertAll(
                            () -> assertThat(event.userId()).isNotNull(),
                            () -> assertThat(event.reservationId()).isNotNull(),
                            () -> assertThat(event.paymentId()).isNotNull()
                    );
                });
    }

}