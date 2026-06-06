package ep.example.auth.features.auth.passwordrecovery.confirm;

import ep.example.auth.domain.PasswordResetToken;
import ep.example.auth.infrastructure.PasswordResetTokenRepository;
import ep.example.auth.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PasswordRecoveryConfirmService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void confirmReset(PasswordRecoveryConfirmRequest request) {
        PasswordResetToken token = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Token inválido"));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token expirado");
        }

        if (token.getUsedAt() != null) {
            throw new IllegalArgumentException("Token ya utilizado");
        }

        token.setUsedAt(LocalDateTime.now());

        var user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        passwordResetTokenRepository.save(token);
        userRepository.save(user);
    }
}
