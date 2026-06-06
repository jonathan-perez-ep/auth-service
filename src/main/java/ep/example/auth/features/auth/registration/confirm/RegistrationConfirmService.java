package ep.example.auth.features.auth.registration.confirm;

import ep.example.auth.domain.AccountConfirmationToken;
import ep.example.auth.domain.User;
import ep.example.auth.infrastructure.AccountConfirmationTokenRepository;
import ep.example.auth.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RegistrationConfirmService {

    private final AccountConfirmationTokenRepository confirmationTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public void confirmAccount(String token) {
        AccountConfirmationToken confirmationToken = confirmationTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido"));

        if (confirmationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token expirado");
        }

        if (confirmationToken.getConfirmedAt() != null) {
            throw new IllegalArgumentException("Token ya utilizado");
        }

        confirmationToken.setConfirmedAt(LocalDateTime.now());

        User user = confirmationToken.getUser();
        user.setAccountConfirmed(true);
        user.setEnabled(true);

        confirmationTokenRepository.save(confirmationToken);
        userRepository.save(user);
    }
}
