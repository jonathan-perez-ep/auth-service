package ep.example.auth.features.account.changepassword;

import ep.example.auth.domain.User;
import ep.example.auth.infrastructure.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChangePasswordServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ChangePasswordService changePasswordService;

    @BeforeEach
    void setupSecurityContext() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("testuser");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void changePassword_withValidCurrentPassword_updatesPassword() {
        User user = User.builder().id(1L).username("testuser").password("hashActual").build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("passwordActual", "hashActual")).thenReturn(true);
        when(passwordEncoder.encode("nuevaPassword")).thenReturn("hashNuevo");

        ChangePasswordRequest request = new ChangePasswordRequest("passwordActual", "nuevaPassword");
        changePasswordService.changePassword(request);

        verify(userRepository).save(user);
        assert user.getPassword().equals("hashNuevo");
    }

    @Test
    void changePassword_withWrongCurrentPassword_throwsIllegalArgumentException() {
        User user = User.builder().id(1L).username("testuser").password("hashActual").build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("passwordIncorrecta", "hashActual")).thenReturn(false);

        ChangePasswordRequest request = new ChangePasswordRequest("passwordIncorrecta", "nuevaPassword");

        assertThatThrownBy(() -> changePasswordService.changePassword(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La contraseña actual es incorrecta");

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void changePassword_withNonExistentUser_throwsIllegalArgumentException() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        ChangePasswordRequest request = new ChangePasswordRequest("cualquiera", "nuevaPassword");

        assertThatThrownBy(() -> changePasswordService.changePassword(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Usuario no encontrado");

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(userRepository, never()).save(any());
    }
}
