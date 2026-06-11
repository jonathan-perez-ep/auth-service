package ep.example.auth.features.auth.passwordrecovery.request;

import ep.example.auth.domain.PasswordResetToken;
import ep.example.auth.domain.User;
import ep.example.auth.infrastructure.PasswordResetTokenRepository;
import ep.example.auth.infrastructure.UserRepository;
import ep.example.auth.shared.email.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordRecoveryServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordRecoveryRepository passwordRecoveryRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private PasswordRecoveryService passwordRecoveryService;

    @Test
    void requestReset_withRegisteredEmail_savesTokenAndSendsEmail() {
        User user = User.builder().id(1L).username("usuario").email("usuario@test.com").build();

        when(userRepository.findByEmail("usuario@test.com")).thenReturn(Optional.of(user));

        passwordRecoveryService.requestReset("usuario@test.com");

        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(eq("usuario@test.com"), any(String.class));
    }

    @Test
    void requestReset_withUnregisteredEmail_doesNothing() {
        when(userRepository.findByEmail("noexiste@test.com")).thenReturn(Optional.empty());

        passwordRecoveryService.requestReset("noexiste@test.com");

        verify(passwordResetTokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(any(), any());
    }

    @Test
    void requestReset_withExistingTokens_invalidatesPreviousTokens() {
        User user = User.builder().id(1L).username("usuario").email("usuario@test.com").build();

        when(userRepository.findByEmail("usuario@test.com")).thenReturn(Optional.of(user));

        passwordRecoveryService.requestReset("usuario@test.com");

        verify(passwordRecoveryRepository).invalidatePendingByUserId(eq(1L), any(LocalDateTime.class));
    }

    @Test
    void requestReset_withRegisteredEmail_savesTokenWithCorrectExpiry() {
        User user = User.builder().id(1L).username("usuario").email("usuario@test.com").build();

        when(userRepository.findByEmail("usuario@test.com")).thenReturn(Optional.of(user));

        passwordRecoveryService.requestReset("usuario@test.com");

        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(captor.capture());

        PasswordResetToken savedToken = captor.getValue();
        assertThat(savedToken.getToken()).isNotBlank();
        assertThat(savedToken.getUsedAt()).isNull();
        assertThat(savedToken.getExpiresAt()).isAfter(LocalDateTime.now());
        assertThat(savedToken.getUser()).isEqualTo(user);
    }
}
