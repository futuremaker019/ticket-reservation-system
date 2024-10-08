package com.reservation.ticket.interfaces.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reservation.ticket.application.dto.criteria.PaymentCriteria;
import com.reservation.ticket.application.usecase.PaymentUsecase;
import com.reservation.ticket.domain.dto.command.QueueCommand;
import com.reservation.ticket.domain.enums.QueueStatus;
import com.reservation.ticket.domain.entity.queue.QueueService;
import com.reservation.ticket.interfaces.dto.PaymentDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@ExtendWith(SpringExtension.class)
class PaymentControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    PaymentUsecase paymentUsecase;
    @MockBean
    QueueService queueService;

    @DisplayName("예약 정보를 이용하여 결재를 생성한다.")
    @Test
    void 예약_정보를_이용하여_결재를_생성한다() throws Exception {
        // given
        String token = "734488355d85";
        Long queueId = 1L;
        QueueCommand.Get get = QueueCommand.Get.of(queueId, token, QueueStatus.ACTIVE);
        given(queueService.getQueueByToken(token)).willReturn(get);

        Long reservationId = 1L;
        PaymentCriteria.Create create = PaymentCriteria.Create.of(reservationId, token);
        willDoNothing().given(paymentUsecase).makePayment(create);

        PaymentDto.Request request = PaymentDto.Request.of(reservationId);

        // when
        mockMvc.perform(post("/api/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request))
                        .header(HttpHeaders.AUTHORIZATION, token)
                )
                .andDo(print())
                .andExpect(status().isOk());

        // then
        then(paymentUsecase).should().makePayment(create);
    }

}