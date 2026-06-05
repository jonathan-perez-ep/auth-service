package ep.example.auth.features.auth.register;

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
public class RegisterController {

    private final RegisterService registerService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody @Valid RegisterRequest request) {
        try {
            registerService.register(request);
            return ResponseEntity.status(201).body("Usuario registrado. Revisa tu email para confirmar tu cuenta.");
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(409).body(ex.getMessage());
        }
    }
}
