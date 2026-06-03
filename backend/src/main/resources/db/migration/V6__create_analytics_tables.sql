CREATE SCHEMA IF NOT EXISTS analytics;

CREATE TABLE analytics.category_counts (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    category     VARCHAR(255) NOT NULL,
    post_count   INTEGER      NOT NULL,
    generated_at TIMESTAMPTZ  NOT NULL
);

CREATE TABLE analytics.daily_post_counts (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    day          DATE         NOT NULL,
    post_count   INTEGER      NOT NULL,
    generated_at TIMESTAMPTZ  NOT NULL
);

CREATE TABLE analytics.top_users (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id      BIGINT       NOT NULL,   -- snapshot value, intentionally NOT a FK to users
    name         VARCHAR(255) NOT NULL,
    email        VARCHAR(255) NOT NULL,
    post_count   INTEGER      NOT NULL,
    generated_at TIMESTAMPTZ  NOT NULL
);

CREATE TABLE analytics.summary (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    total_posts    INTEGER     NOT NULL,
    total_comments INTEGER     NOT NULL,
    total_users    INTEGER     NOT NULL,
    generated_at   TIMESTAMPTZ NOT NULL
);


CREATE INDEX idx_category_counts_generated_at   ON analytics.category_counts (generated_at);
CREATE INDEX idx_daily_post_counts_generated_at ON analytics.daily_post_counts (generated_at);
CREATE INDEX idx_top_users_generated_at         ON analytics.top_users (generated_at);
CREATE INDEX idx_summary_generated_at           ON analytics.summary (generated_at);

CREATE INDEX idx_daily_post_counts_day ON analytics.daily_post_counts (day);