package ep.example.auth.features.auth.passwordrecovery.request;

import ep.example.auth.domain.PasswordResetToken;
import ep.example.auth.infrastructure.PasswordResetTokenRepository;
import ep.example.auth.infrastructure.UserRepository;
import ep.example.auth.shared.email.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordRecoveryService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;

    @Transactional
    public void requestReset(String email) {
        // No revelamos si el email existe — previene enumeración de usuarios
        userRepository.findByEmail(email).ifPresent(user -> {
            // Invalidar tokens anteriores para este usuario
            passwordResetTokenRepository.findAllByUser(user)
                    .forEach(t -> t.setUsedAt(LocalDateTime.now()));

            String tokenValue = UUID.randomUUID().toString();

            passwordResetTokenRepository.save(PasswordResetToken.builder()
                    .token(tokenValue)
                    .user(user)
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .build());

            emailService.sendPasswordResetEmail(email, tokenValue);
        });
    }
}
