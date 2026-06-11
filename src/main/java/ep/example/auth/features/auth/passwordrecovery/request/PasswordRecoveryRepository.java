package ep.example.auth.features.auth.passwordrecovery.request;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
class PasswordRecoveryRepository {

    private final JdbcTemplate jdbcTemplate;

    void invalidatePendingByUserId(long userId, LocalDateTime now) {
        jdbcTemplate.update(
                "UPDATE password_reset_tokens SET used_at = ? WHERE user_id = ? AND used_at IS NULL",
                now, userId
        );
    }
}
