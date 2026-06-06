CREATE TABLE password_reset_tokens (
    id         bigserial    PRIMARY KEY,
    token      varchar(255) NOT NULL UNIQUE,
    user_id    bigint       NOT NULL REFERENCES users(id),
    expires_at timestamp    NOT NULL,
    used_at    timestamp    NULL
);

CREATE INDEX idx_password_reset_tokens_token ON password_reset_tokens(token);
