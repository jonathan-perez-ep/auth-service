package ep.example.auth.features.auth.passwordrecovery.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PasswordRecoveryRequest {

    @NotBlank
    @Email
    private String email;
}
