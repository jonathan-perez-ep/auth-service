package ep.example.auth.features.auth.register;

import ep.example.auth.domain.ConfirmationToken;
import ep.example.auth.domain.User;
import ep.example.auth.domain.UserRoleEnum;
import ep.example.auth.infrastructure.ConfirmationTokenRepository;
import ep.example.auth.infrastructure.UserRepository;
import ep.example.auth.shared.email.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ConfirmationTokenRepository confirmationTokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private RegisterService registerService;

    @Test
    void register_withValidUser_savesUserAndSendsEmail() {
        RegisterRequest request = new RegisterRequest("nuevoUsuario", "nuevo@email.com", "password123");

        when(userRepository.existsByUsername("nuevoUsuario")).thenReturn(false);
        when(userRepository.existsByEmail("nuevo@email.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("passwordCodificado");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        registerService.register(request);

        verify(userRepository).save(any(User.class));
        verify(confirmationTokenRepository).save(any(ConfirmationToken.class));
        verify(emailService).sendConfirmationEmail(eq("nuevo@email.com"), anyString());
    }

    @Test
    void register_withExistingUsername_throwsIllegalArgumentException() {
        RegisterRequest request = new RegisterRequest("usuarioExistente", "otro@email.com", "password123");

        when(userRepository.existsByUsername("usuarioExistente")).thenReturn(true);

        assertThatThrownBy(() -> registerService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El nombre de usuario ya está en uso");

        verify(userRepository, never()).save(any(User.class));
        verify(confirmationTokenRepository, never()).save(any(ConfirmationToken.class));
        verify(emailService, never()).sendConfirmationEmail(anyString(), anyString());
    }

    @Test
    void register_withExistingEmail_throwsIllegalArgumentException() {
        RegisterRequest request = new RegisterRequest("otroUsuario", "emailExistente@email.com", "password123");

        when(userRepository.existsByUsername("otroUsuario")).thenReturn(false);
        when(userRepository.existsByEmail("emailExistente@email.com")).thenReturn(true);

        assertThatThrownBy(() -> registerService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El correo electrónico ya está registrado");

        verify(userRepository, never()).save(any(User.class));
        verify(confirmationTokenRepository, never()).save(any(ConfirmationToken.class));
        verify(emailService, never()).sendConfirmationEmail(anyString(), anyString());
    }

    @Test
    void register_withValidUser_encodesPassword() {
        RegisterRequest request = new RegisterRequest("nuevoUsuario", "nuevo@email.com", "miPasswordSegura");

        when(userRepository.existsByUsername("nuevoUsuario")).thenReturn(false);
        when(userRepository.existsByEmail("nuevo@email.com")).thenReturn(false);
        when(passwordEncoder.encode("miPasswordSegura")).thenReturn("hashEncriptado");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        registerService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(passwordEncoder).encode("miPasswordSegura");
        verify(userRepository).save(userCaptor.capture());

        User usuarioGuardado = userCaptor.getValue();
        assertThat(usuarioGuardado.getPassword()).isEqualTo("hashEncriptado");
        assertThat(usuarioGuardado.getRole()).isEqualTo(UserRoleEnum.USER);
        assertThat(usuarioGuardado.isAccountConfirmed()).isFalse();
        assertThat(usuarioGuardado.isEnabled()).isFalse();
    }
}
