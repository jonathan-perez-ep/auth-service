package ep.example.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthorizationServerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void clientCredentials_conClienteValido_retornaToken() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                        .header("Authorization", "Basic ZGVtby1jbGllbnQ6c2VjcmV0")
                        .param("grant_type", "client_credentials")
                        .param("scope", "read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.scope").value("read"));
    }

    @Test
    void clientCredentials_conClienteInvalido_retorna401() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                        .header("Authorization", "Basic Y2xpZW50ZS1mYWxzbzpjbGF2ZS1mYWxzYQ==")
                        .param("grant_type", "client_credentials")
                        .param("scope", "read"))
                .andExpect(status().isUnauthorized());
    }

    // Verifica que el endpoint público de descubrimiento responde con el issuer correcto.
    // Cualquier cliente OAuth2 (Angular, Postman, otro microservicio) lee este endpoint
    // para auto-configurarse — si falla, ningún cliente podrá conectarse al AS.
    @Test
    void discoveryDocument_retornaIssuerCorrecto() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").value("http://localhost:9000"))
                .andExpect(jsonPath("$.token_endpoint").exists())
                .andExpect(jsonPath("$.jwks_uri").exists());
    }
}
