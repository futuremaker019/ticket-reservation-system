package com.reservation.ticket.interfaces.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reservation.ticket.domain.dto.command.PointCommand;
import com.reservation.ticket.domain.dto.command.QueueCommand;
import com.reservation.ticket.domain.enums.QueueStatus;
import com.reservation.ticket.domain.entity.point.PointService;
import com.reservation.ticket.domain.entity.queue.QueueService;
import com.reservation.ticket.interfaces.dto.PointDto;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PointController.class)
@ExtendWith(SpringExtension.class)
class PointControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    PointService pointService;
    @MockBean
    QueueService queueService;

    @DisplayName("토큰을 이용하여 사용자가 가진 포인트를 조회한다.")
    @Test
    void 토큰을_이용하여_사용자가_가진_포인트를_조회한다() throws Exception {
        // given
        String token = "734488355d85";
        Long queueId = 1L;
        QueueCommand.Get get = QueueCommand.Get.of(queueId, token, QueueStatus.ACTIVE);
        given(queueService.getQueueByToken(token)).willReturn(get);

        int point = 100;
        PointCommand.Get pointCommand = PointCommand.Get.of(point);
        given(pointService.getPoint(token)).willReturn(pointCommand);

        PointDto.Response response = PointDto.Response.from(pointCommand);

        // when
        mockMvc.perform(get("/api/point")
                        .header(HttpHeaders.AUTHORIZATION, token)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().bytes(objectMapper.writeValueAsBytes(response)));

        // then
        then(pointService).should().getPoint(token);
    }

    @DisplayName("토큰과 충전할 포인트를 전달받아 포인트를 충전한다.")
    @Test
    void 토큰과_충전할_포인트를_전달받아_포인트를_충전한다() throws Exception {
        // given
        int point = 100;
        String token = "734488355d85";
        willDoNothing().given(pointService).chargePoint(point, token);

        // when
        mockMvc.perform(post("/api/point")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(PointDto.Request.of(point)))
                        .header(HttpHeaders.AUTHORIZATION, token)
                )
                .andDo(print())
                .andExpect(status().isOk());

        // then
        then(pointService).should().chargePoint(point, token);
    }
}