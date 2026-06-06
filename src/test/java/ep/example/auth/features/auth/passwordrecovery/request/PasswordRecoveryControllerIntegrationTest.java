package ep.example.auth.features.auth.passwordrecovery.request;

import ep.example.auth.domain.User;
import ep.example.auth.domain.UserRoleEnum;
import ep.example.auth.infrastructure.PasswordResetTokenRepository;
import ep.example.auth.infrastructure.UserRepository;
import ep.example.auth.shared.email.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordRecoveryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private EmailService emailService;

    @BeforeEach
    void cleanup() {
        passwordResetTokenRepository.deleteAll();
        userRepository.findByEmail("recovery-test@test.com").ifPresent(userRepository::delete);
    }

    private User crearUsuario() {
        return userRepository.save(User.builder()
                .username("recovery-test-user")
                .email("recovery-test@test.com")
                .password(passwordEncoder.encode("password123"))
                .role(UserRoleEnum.USER)
                .enabled(true)
                .accountConfirmed(true)
                .build());
    }

    @Test
    void requestReset_withRegisteredEmail_returns200() throws Exception {
        crearUsuario();

        mockMvc.perform(post("/auth/password-recovery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "recovery-test@test.com" }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        "Si el email está registrado, recibirás un correo con instrucciones."));
    }

    @Test
    void requestReset_withUnregisteredEmail_returns200() throws Exception {
        mockMvc.perform(post("/auth/password-recovery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "noexiste@test.com" }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        "Si el email está registrado, recibirás un correo con instrucciones."));
    }

    @Test
    void requestReset_withInvalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/auth/password-recovery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "no-es-un-email" }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requestReset_withBlankEmail_returns400() throws Exception {
        mockMvc.perform(post("/auth/password-recovery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "" }
                                """))
                .andExpect(status().isBadRequest());
    }
}
