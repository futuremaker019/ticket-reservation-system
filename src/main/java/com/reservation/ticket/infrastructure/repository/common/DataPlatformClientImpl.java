package com.reservation.ticket.infrastructure.repository.common;

import com.reservation.ticket.domain.common.DataPlatformClient;
import org.springframework.stereotype.Component;

@Component
public class DataPlatformClientImpl implements DataPlatformClient {

    @Override
    public boolean send(Long id) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

}
