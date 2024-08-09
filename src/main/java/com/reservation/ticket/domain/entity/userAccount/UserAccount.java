package com.reservation.ticket.domain.entity.userAccount;

import com.reservation.ticket.domain.exception.ApplicationException;
import com.reservation.ticket.domain.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.DynamicUpdate;

import java.util.Objects;

@Getter
@Entity
@DynamicUpdate
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAccount {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "VARCHAR(20) NOT NULL")
    private String username;

    private String token;
    private int point;

    @Version
    private Long version;

    public UserAccount(Long id) {
        this.id = id;
    }

    public UserAccount(Long id, String username) {
        this.id = id;
        this.username = username;
    }

    public UserAccount(Long id, String username, String token, int point) {
        this.id = id;
        this.username = username;
        this.token = token;
        this.point = point;
    }

    public static UserAccount of(Long id) {
        return new UserAccount(id);
    }

    public static UserAccount of(Long id, String username) {
        return new UserAccount(id, username);
    }

    public static UserAccount of(Long id, int point) {
        return new UserAccount(id, null, null, point);
    }

    public static UserAccount of(Long id, String username, String token) {
        return new UserAccount(id, username, token, 0);
    }

    public static UserAccount of(Long id, String username, String token, int point) {
        return new UserAccount(id, username, token, point);
    }

    public void saveToken(String token) {
        this.token = token;
    }

    public void chargePoint(int point) {
        if (point <= 0) {
            throw new ApplicationException(ErrorCode.INVALID_POINT,
                    "Invalid chargeable point : %d".formatted(point));
        }
        this.point = point;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserAccount userAccount)) return false;
        return id != null && id.equals(userAccount.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
