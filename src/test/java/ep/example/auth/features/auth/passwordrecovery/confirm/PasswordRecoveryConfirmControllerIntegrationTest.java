package ep.example.auth.features.auth.passwordrecovery.confirm;

import ep.example.auth.domain.PasswordResetToken;
import ep.example.auth.domain.User;
import ep.example.auth.domain.UserRoleEnum;
import ep.example.auth.infrastructure.PasswordResetTokenRepository;
import ep.example.auth.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordRecoveryConfirmControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void cleanup() {
        passwordResetTokenRepository.deleteAll();
        userRepository.findByEmail("confirm-recovery@test.com").ifPresent(userRepository::delete);

        testUser = userRepository.save(User.builder()
                .username("confirm-recovery-user")
                .email("confirm-recovery@test.com")
                .password(passwordEncoder.encode("passwordViejo123"))
                .role(UserRoleEnum.USER)
                .enabled(true)
                .accountConfirmed(true)
                .build());
    }

    private PasswordResetToken crearToken(String tokenValue, LocalDateTime expiresAt, LocalDateTime usedAt) {
        return passwordResetTokenRepository.save(PasswordResetToken.builder()
                .token(tokenValue)
                .user(testUser)
                .expiresAt(expiresAt)
                .usedAt(usedAt)
                .build());
    }

    @Test
    void confirmReset_withValidToken_returns200() throws Exception {
        crearToken("token-valido", LocalDateTime.now().plusHours(1), null);

        mockMvc.perform(post("/auth/password-recovery/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "token-valido",
                                  "newPassword": "nuevaPassword123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string("Contraseña actualizada exitosamente."));
    }

    @Test
    void confirmReset_withInvalidToken_returns400() throws Exception {
        mockMvc.perform(post("/auth/password-recovery/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "token-inexistente",
                                  "newPassword": "nuevaPassword123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Token inválido"));
    }

    @Test
    void confirmReset_withExpiredToken_returns400() throws Exception {
        crearToken("token-expirado", LocalDateTime.now().minusHours(1), null);

        mockMvc.perform(post("/auth/password-recovery/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "token-expirado",
                                  "newPassword": "nuevaPassword123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Token expirado"));
    }

    @Test
    void confirmReset_withUsedToken_returns400() throws Exception {
        crearToken("token-ya-usado", LocalDateTime.now().plusHours(1), LocalDateTime.now().minusMinutes(10));

        mockMvc.perform(post("/auth/password-recovery/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "token-ya-usado",
                                  "newPassword": "nuevaPassword123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Token ya utilizado"));
    }

    @Test
    void confirmReset_withBlankToken_returns400() throws Exception {
        mockMvc.perform(post("/auth/password-recovery/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "",
                                  "newPassword": "nuevaPassword123"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void confirmReset_withShortPassword_returns400() throws Exception {
        crearToken("token-valido-2", LocalDateTime.now().plusHours(1), null);

        mockMvc.perform(post("/auth/password-recovery/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "token-valido-2",
                                  "newPassword": "123"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
