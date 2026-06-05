package ep.example.auth.features.auth.confirm;

import ep.example.auth.domain.ConfirmationToken;
import ep.example.auth.domain.User;
import ep.example.auth.infrastructure.ConfirmationTokenRepository;
import ep.example.auth.infrastructure.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfirmServiceTest {

    @Mock
    private ConfirmationTokenRepository confirmationTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ConfirmService confirmService;

    @Test
    void confirmAccount_tokenValido_activaCuenta() {
        User usuario = User.builder()
                .id(1L)
                .username("usuario")
                .password("password")
                .enabled(false)
                .accountConfirmed(false)
                .build();

        ConfirmationToken token = ConfirmationToken.builder()
                .id(1L)
                .token("token-valido")
                .user(usuario)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .confirmedAt(null)
                .build();

        when(confirmationTokenRepository.findByToken("token-valido")).thenReturn(Optional.of(token));

        confirmService.confirmAccount("token-valido");

        assertThat(token.getConfirmedAt()).isNotNull();
        assertThat(usuario.isEnabled()).isTrue();
        assertThat(usuario.isAccountConfirmed()).isTrue();

        verify(confirmationTokenRepository, times(1)).save(token);
        verify(userRepository, times(1)).save(usuario);
    }

    @Test
    void confirmAccount_tokenInvalido_lanzaIllegalArgumentException() {
        when(confirmationTokenRepository.findByToken("token-inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> confirmService.confirmAccount("token-inexistente"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Token inválido");

        verify(confirmationTokenRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void confirmAccount_tokenExpirado_lanzaIllegalArgumentException() {
        User usuario = User.builder()
                .id(2L)
                .username("usuario")
                .password("password")
                .build();

        ConfirmationToken token = ConfirmationToken.builder()
                .id(2L)
                .token("token-expirado")
                .user(usuario)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .confirmedAt(null)
                .build();

        when(confirmationTokenRepository.findByToken("token-expirado")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> confirmService.confirmAccount("token-expirado"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Token expirado");

        verify(confirmationTokenRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void confirmAccount_tokenYaUtilizado_lanzaIllegalArgumentException() {
        User usuario = User.builder()
                .id(3L)
                .username("usuario")
                .password("password")
                .build();

        ConfirmationToken token = ConfirmationToken.builder()
                .id(3L)
                .token("token-ya-usado")
                .user(usuario)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .confirmedAt(LocalDateTime.now().minusMinutes(10))
                .build();

        when(confirmationTokenRepository.findByToken("token-ya-usado")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> confirmService.confirmAccount("token-ya-usado"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Token ya utilizado");

        verify(confirmationTokenRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }
}
