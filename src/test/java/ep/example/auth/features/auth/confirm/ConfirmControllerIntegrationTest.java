package ep.example.auth.features.auth.confirm;

import ep.example.auth.domain.AccountConfirmationToken;
import ep.example.auth.domain.User;
import ep.example.auth.domain.UserRoleEnum;
import ep.example.auth.infrastructure.AccountConfirmationTokenRepository;
import ep.example.auth.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConfirmControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountConfirmationTokenRepository confirmationTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanup() {
        confirmationTokenRepository.deleteAll();
        userRepository.findByUsername("confirm-test-valido").ifPresent(userRepository::delete);
        userRepository.findByUsername("confirm-test-expirado").ifPresent(userRepository::delete);
        userRepository.findByUsername("confirm-test-usado").ifPresent(userRepository::delete);
    }

    private User crearUsuario(String username) {
        User user = User.builder()
                .username(username)
                .email(username + "@test.com")
                .password(passwordEncoder.encode("password123"))
                .role(UserRoleEnum.USER)
                .enabled(false)
                .accountConfirmed(false)
                .build();
        return userRepository.save(user);
    }

    @Test
    void confirm_withValidToken_returns200() throws Exception {
        User user = crearUsuario("confirm-test-valido");
        confirmationTokenRepository.save(AccountConfirmationToken.builder()
                .token("token-valido")
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .confirmedAt(null)
                .build());

        mockMvc.perform(get("/auth/confirm")
                        .param("token", "token-valido"))
                .andExpect(status().isOk())
                .andExpect(content().string("Cuenta confirmada exitosamente."));
    }

    @Test
    void confirm_withInvalidToken_returns400() throws Exception {
        mockMvc.perform(get("/auth/confirm")
                        .param("token", "token-inexistente"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Token inválido"));
    }

    @Test
    void confirm_withExpiredToken_returns400() throws Exception {
        User user = crearUsuario("confirm-test-expirado");
        confirmationTokenRepository.save(AccountConfirmationToken.builder()
                .token("token-expirado")
                .user(user)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .confirmedAt(null)
                .build());

        mockMvc.perform(get("/auth/confirm")
                        .param("token", "token-expirado"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Token expirado"));
    }

    @Test
    void confirm_withAlreadyUsedToken_returns400() throws Exception {
        User user = crearUsuario("confirm-test-usado");
        confirmationTokenRepository.save(AccountConfirmationToken.builder()
                .token("token-ya-usado")
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .confirmedAt(LocalDateTime.now().minusMinutes(10))
                .build());

        mockMvc.perform(get("/auth/confirm")
                        .param("token", "token-ya-usado"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Token ya utilizado"));
    }
}
