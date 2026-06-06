package ep.example.auth.features.auth.passwordrecovery.confirm;

import ep.example.auth.domain.PasswordResetToken;
import ep.example.auth.domain.User;
import ep.example.auth.infrastructure.PasswordResetTokenRepository;
import ep.example.auth.infrastructure.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordRecoveryConfirmServiceTest {

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordRecoveryConfirmService passwordRecoveryConfirmService;

    @Test
    void confirmReset_withValidToken_updatesPasswordAndMarksTokenUsed() {
        User user = User.builder().id(1L).username("usuario").password("passwordViejo").build();
        PasswordResetToken token = PasswordResetToken.builder()
                .token("token-valido").user(user)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .usedAt(null).build();

        when(passwordResetTokenRepository.findByToken("token-valido")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("nuevaPassword123")).thenReturn("hashNuevo");

        PasswordRecoveryConfirmRequest request =
                new PasswordRecoveryConfirmRequest("token-valido", "nuevaPassword123");

        passwordRecoveryConfirmService.confirmReset(request);

        assertThat(token.getUsedAt()).isNotNull();
        assertThat(user.getPassword()).isEqualTo("hashNuevo");
        verify(passwordResetTokenRepository).save(token);
        verify(userRepository).save(user);
    }

    @Test
    void confirmReset_withInvalidToken_throwsIllegalArgumentException() {
        when(passwordResetTokenRepository.findByToken("token-inexistente")).thenReturn(Optional.empty());

        PasswordRecoveryConfirmRequest request =
                new PasswordRecoveryConfirmRequest("token-inexistente", "nuevaPassword123");

        assertThatThrownBy(() -> passwordRecoveryConfirmService.confirmReset(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Token inválido");

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void confirmReset_withExpiredToken_throwsIllegalArgumentException() {
        User user = User.builder().id(1L).username("usuario").password("passwordViejo").build();
        PasswordResetToken token = PasswordResetToken.builder()
                .token("token-expirado").user(user)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .usedAt(null).build();

        when(passwordResetTokenRepository.findByToken("token-expirado")).thenReturn(Optional.of(token));

        PasswordRecoveryConfirmRequest request =
                new PasswordRecoveryConfirmRequest("token-expirado", "nuevaPassword123");

        assertThatThrownBy(() -> passwordRecoveryConfirmService.confirmReset(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Token expirado");

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void confirmReset_withUsedToken_throwsIllegalArgumentException() {
        User user = User.builder().id(1L).username("usuario").password("passwordViejo").build();
        PasswordResetToken token = PasswordResetToken.builder()
                .token("token-ya-usado").user(user)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .usedAt(LocalDateTime.now().minusMinutes(10)).build();

        when(passwordResetTokenRepository.findByToken("token-ya-usado")).thenReturn(Optional.of(token));

        PasswordRecoveryConfirmRequest request =
                new PasswordRecoveryConfirmRequest("token-ya-usado", "nuevaPassword123");

        assertThatThrownBy(() -> passwordRecoveryConfirmService.confirmReset(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Token ya utilizado");

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }
}
