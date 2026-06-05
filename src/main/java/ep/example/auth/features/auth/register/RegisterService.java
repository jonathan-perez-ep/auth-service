package ep.example.auth.features.auth.register;

import ep.example.auth.domain.ConfirmationToken;
import ep.example.auth.domain.User;
import ep.example.auth.domain.UserRoleEnum;
import ep.example.auth.infrastructure.ConfirmationTokenRepository;
import ep.example.auth.infrastructure.UserRepository;
import ep.example.auth.shared.email.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegisterService {

    private final UserRepository userRepository;
    private final ConfirmationTokenRepository confirmationTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("El nombre de usuario ya está en uso");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("El correo electrónico ya está registrado");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRoleEnum.USER)
                .accountConfirmed(false)
                .enabled(false)
                .build();

        userRepository.save(user);

        String tokenValue = UUID.randomUUID().toString();

        ConfirmationToken confirmationToken = ConfirmationToken.builder()
                .token(tokenValue)
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        confirmationTokenRepository.save(confirmationToken);
        emailService.sendConfirmationEmail(user.getEmail(), tokenValue);
    }
}
