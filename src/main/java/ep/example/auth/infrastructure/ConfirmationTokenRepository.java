package ep.example.auth.infrastructure;

import ep.example.auth.domain.ConfirmationToken;
import ep.example.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConfirmationTokenRepository extends JpaRepository<ConfirmationToken, Long> {

    Optional<ConfirmationToken> findByToken(String token);

    List<ConfirmationToken> findAllByUser(User user);
}
