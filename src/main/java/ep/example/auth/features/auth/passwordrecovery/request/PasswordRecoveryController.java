package ep.example.auth.features.auth.passwordrecovery.request;

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
public class PasswordRecoveryController {

    private final PasswordRecoveryService passwordRecoveryService;

    @PostMapping("/password-recovery")
    public ResponseEntity<String> requestReset(@RequestBody @Valid PasswordRecoveryRequest request) {
        passwordRecoveryService.requestReset(request.getEmail());
        return ResponseEntity.ok("Si el email está registrado, recibirás un correo con instrucciones.");
    }
}
