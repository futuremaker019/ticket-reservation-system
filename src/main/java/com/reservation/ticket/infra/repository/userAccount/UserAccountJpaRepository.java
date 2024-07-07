package com.reservation.ticket.infra.repository.userAccount;

import com.reservation.ticket.domain.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountJpaRepository extends JpaRepository<UserAccount, Long> {
}
