
CREATE TABLE users (
    id         BIGSERIAL PRIMARY KEY,
    email      VARCHAR(255) NOT NULL UNIQUE,
    name       VARCHAR(255) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(50)  NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE posts (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    slug        VARCHAR(255) NOT NULL UNIQUE,
    content     TEXT NOT NULL,
    category_id BIGINT REFERENCES categories (id),
    author_id   BIGINT NOT NULL REFERENCES users (id),
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP
);

CREATE TABLE comments (
    id         BIGSERIAL PRIMARY KEY,
    content    TEXT NOT NULL,
    post_id    BIGINT NOT NULL REFERENCES posts (id),
    author_id  BIGINT NOT NULL REFERENCES users (id),
    created_at TIMESTAMP
);
