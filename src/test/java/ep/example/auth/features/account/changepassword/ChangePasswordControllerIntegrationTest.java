package ep.example.auth.features.account.changepassword;

import ep.example.auth.domain.User;
import ep.example.auth.domain.UserRoleEnum;
import ep.example.auth.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChangePasswordControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String TEST_USERNAME = "change-pw-test-user";
    private static final String CURRENT_PASSWORD = "currentPassword123";

    @BeforeEach
    void setup() {
        userRepository.findByUsername(TEST_USERNAME).ifPresent(userRepository::delete);

        userRepository.save(User.builder()
                .username(TEST_USERNAME)
                .email("change-pw@test.com")
                .password(passwordEncoder.encode(CURRENT_PASSWORD))
                .role(UserRoleEnum.USER)
                .enabled(true)
                .accountConfirmed(true)
                .build());
    }

    @Test
    @WithMockUser(username = TEST_USERNAME)
    void changePassword_withValidCurrentPassword_returns200() throws Exception {
        mockMvc.perform(post("/account/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "currentPassword123",
                                  "newPassword": "newPassword456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string("Contraseña actualizada exitosamente."));
    }

    @Test
    @WithMockUser(username = TEST_USERNAME)
    void changePassword_withWrongCurrentPassword_returns400() throws Exception {
        mockMvc.perform(post("/account/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "passwordIncorrecta",
                                  "newPassword": "newPassword456"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("La contraseña actual es incorrecta"));
    }

    @Test
    @WithMockUser(username = TEST_USERNAME)
    void changePassword_withBlankCurrentPassword_returns400() throws Exception {
        mockMvc.perform(post("/account/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "",
                                  "newPassword": "newPassword456"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = TEST_USERNAME)
    void changePassword_withShortNewPassword_returns400() throws Exception {
        mockMvc.perform(post("/account/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "currentPassword123",
                                  "newPassword": "123"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changePassword_withoutAuthentication_returns3xxRedirect() throws Exception {
        mockMvc.perform(post("/account/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "currentPassword123",
                                  "newPassword": "newPassword456"
                                }
                                """))
                .andExpect(status().is3xxRedirection());
    }
}
