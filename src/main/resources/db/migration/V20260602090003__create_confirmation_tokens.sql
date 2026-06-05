CREATE TABLE confirmation_tokens (
    id          bigserial    PRIMARY KEY,
    token       varchar(255) NOT NULL UNIQUE,
    user_id     bigint       NOT NULL REFERENCES users(id),
    expires_at  timestamp    NOT NULL,
    confirmed_at timestamp   NULL
);

CREATE INDEX idx_confirmation_tokens_token ON confirmation_tokens(token);
