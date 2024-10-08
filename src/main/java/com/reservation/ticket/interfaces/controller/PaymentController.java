package com.reservation.ticket.interfaces.controller;

import com.reservation.ticket.application.dto.criteria.PaymentCriteria;
import com.reservation.ticket.application.usecase.PaymentUsecase;
import com.reservation.ticket.interfaces.dto.PaymentDto;
import com.reservation.ticket.interfaces.dto.Response;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentUsecase paymentUsecase;

    /**
     * 예약 정보를 이용하여 결재를 생성한다.
     */
    @PostMapping
    public Response<Void> makePaymentDone(
            @RequestBody PaymentDto.Request request, HttpServletRequest servletRequest
    ) {
        String token = servletRequest.getHeader(HttpHeaders.AUTHORIZATION);
        paymentUsecase.makePayment(PaymentCriteria.Create.of(request.reservationId(), token));
        return Response.success();
    }

}

