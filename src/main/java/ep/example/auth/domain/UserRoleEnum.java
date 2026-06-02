package ep.example.auth.domain;

// Sin prefijo ROLE_ — Spring Security lo agrega automáticamente en hasRole("ADMIN")
public enum UserRoleEnum {
    USER,
    ADMIN
}
