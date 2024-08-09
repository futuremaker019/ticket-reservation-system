package com.reservation.ticket.domain.entity.point;

import com.reservation.ticket.domain.dto.command.PointCommand;
import com.reservation.ticket.domain.entity.point.pointhistory.PointHistory;
import com.reservation.ticket.domain.entity.point.pointhistory.PointHistoryRepository;
import com.reservation.ticket.domain.entity.userAccount.UserAccount;
import com.reservation.ticket.domain.enums.TransactionType;
import com.reservation.ticket.domain.entity.userAccount.UserAccountRepository;
import com.reservation.ticket.domain.exception.ApplicationException;
import com.reservation.ticket.domain.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserAccountRepository userAccountRepository;
    private final PointHistoryRepository pointHistoryRepository;

    /**
     * 포인트 조회
     */
    @Transactional
    public PointCommand.Get getPoint(String token) {
        UserAccount user = userAccountRepository.findByToken(token);
        return PointCommand.Get.of(user.getPoint());
    }

    /**
     * 포인트 사용 - 포인트가 결제할 금액보다 작을 경우 예외발생
     */
    @Transactional
    public void usePoint(int reservedPrice, UserAccount userAccount) {
        if (reservedPrice > userAccount.getPoint()) {
            throw new ApplicationException(ErrorCode.NOT_ENOUGH_POINT,
                    "not enough point for price - point : %d".formatted(userAccount.getPoint()));
        }

        // 사용자 포인트 삭감
        int restPoint = userAccount.getPoint() - reservedPrice;
        userAccount.chargePoint(restPoint);

        // 포인트 히스토리 저장
        PointHistory pointHistory = PointHistory.of(userAccount, TransactionType.USE, restPoint);
        pointHistoryRepository.save(pointHistory);
    }

    /**
     * 포인트 충전 - 포인트가 0이거나 0이하면 예외 발생
     */
    public void chargePoint(int point, String token) {
        UserAccount userAccount = userAccountRepository.findByToken(token);
        int chargeablePoint = userAccount.getPoint() + point;
        userAccount.chargePoint(chargeablePoint);
        userAccountRepository.save(userAccount);
    }
}
