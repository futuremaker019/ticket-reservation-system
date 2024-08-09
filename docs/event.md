### Transaction 범위 수정을 위한 Event 적용

목적
- `PaymentUsecase` 의 `makePayment` 메서드의 Transaction 범위를 수정할 수 있도록 스프링에서 제공하는 Application Event 를 적용함

### 결재 코드수정
- 결제시 예약의 결제상태 변경, 포인트 차감, 결제정보 저장, 토큰 만료의 과정을 서로 다른 Transaction으로 구성되도록 한다.
- 예약의 결제상태 변경, 결재정보 저장은 각 도메인의 메서드 내부에서 이루어저며, Active 토큰을 삭제함으로써 만료시킨다.
- 포인트는 스프링에서 제공하는 이벤트를 사용하여 이벤트 처리하였다. (코드 아래에 이벤트 내용 이어짐)
- 데이터 플랫폼에 정보 전송 실패시 retry 를 한번 더 할 수 있도록 구성하였다.

```java
class PaymentUsecase {

    public void makePayment(Long reservationId, String token) {
        UserAccount userAccount = userAccountService.getUserAccountByToken(token);
        Reservation reservation = reservationService.getReservation(reservationId);
        // 예약완료를 위한 결제정보 등록
        Payment payment = paymentService.createPayment(reservation, userAccount);
        // 대기열 토큰 검증 및 만료 - redis에 저장된 ACTIVE 토큰 삭제
        queueRedisService.expire(token);

        /**
         * 포인트 차감 이벤트
         *  - 포인트가 예약금액 보다 적다면 예외를 발생시킴
         */
        publisher.publishEvent(
                PointEvent.Use.of(
                        userAccount.getId(), userAccount.getPoint(), reservation.getPrice(),
                        reservation.getId(), payment.getId()
                )
        );

        // 데이터 플랫폼으로 예약정보 전송
        callDataPlatform(reservation.getId());
    }

    /**
     * 호출시 retry 되도록 함
     */
    @Retryable(
            maxAttempts = 2,                    // 최대 시도 횟수
            backoff = @Backoff(delay = 1000)    // 재시도 간격
    )
    public void callDataPlatform(Long reservationId) {
        boolean result = dataPlatformClient.send(reservationId);
        if (!result) {
            log.warn("data platform send failed");
        }
    }
}
```

#### 이벤트 적용

- 포인트 차감시 사용자의 포인트보다 예약금액이 초과된다면 예외를 발생시킨다.
- 발생된 예외를 확인하여 catch 문에서 보상 트랜잭션 이벤트를 발생한다.
- 이벤트는 payment의 이벤트로 예약의 결재상태 원복, 생성된 결제정보 삭제, 삭제된 Active 토큰 재생성으로 구성된다.
- 보상 트랜잭션을 이벤트로 처리한 이유는 추후 콘서트, 결제 도메인을 `MSA`로 분리시 처리할 수 있도록 적용했다.
- 또한 `phase`의 `AFTER_COMMIT`을 적용하여 이벤트를 호출하는 메서드의 처리를 우선하여 보상 트랜잭션이 유효하게 동작하도록 하였다.

```java
@Component
@RequiredArgsConstructor
public class PointListener {

    private final PointService pointService;
    private final ApplicationEventPublisher publisher;

    /**
     * 예약금액을 포인트로 차감한다.
     *  포인트가 부족시 보상 트랜잭션을 발행한다.
     *      (publish payment recover event - `MSA`를 위해 event 형태로 처리)
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void usePointEvent(PointEvent.Use event) {
        try {
            pointService.usePoint(
                    event.price(), UserAccount.of(event.userId(), event.userPoint())
            );
        } catch (Exception e) {
            publisher.publishEvent(
                    PaymentEvent.Recover.of(
                            event.reservationId(),
                            event.paymentId(),
                            event.userId()
                    )
            );
            throw e;
        }
    }

}
```