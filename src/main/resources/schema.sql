CREATE TABLE wallet (
    id          BIGINT PRIMARY KEY,
    address     VARCHAR(64) UNIQUE NOT NULL,
    nonce       BIGINT NOT NULL,
    balance     DECIMAL(64, 20) NOT NULL
);
