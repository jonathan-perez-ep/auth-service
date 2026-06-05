package ep.example.auth.infrastructure;

import ep.example.auth.domain.AccountConfirmationToken;
import ep.example.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountConfirmationTokenRepository extends JpaRepository<AccountConfirmationToken, Long> {

    Optional<AccountConfirmationToken> findByToken(String token);

    List<AccountConfirmationToken> findAllByUser(User user);
}
