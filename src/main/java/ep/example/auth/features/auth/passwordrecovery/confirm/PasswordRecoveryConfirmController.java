package ep.example.auth.features.auth.passwordrecovery.confirm;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class PasswordRecoveryConfirmController {

    private final PasswordRecoveryConfirmService passwordRecoveryConfirmService;

    @PostMapping("/password-recovery/confirm")
    public ResponseEntity<String> confirmReset(@RequestBody @Valid PasswordRecoveryConfirmRequest request) {
        passwordRecoveryConfirmService.confirmReset(request);
        return ResponseEntity.ok("Contraseña actualizada exitosamente.");
    }
}
