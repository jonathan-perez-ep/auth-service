package ep.example.auth.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Builder.Default
    private boolean enabled = true;

    // Rol del usuario: ROLE_USER, ROLE_ADMIN, etc.
    @Column(nullable = false)
    @Builder.Default
    private String role = "ROLE_USER";
}
