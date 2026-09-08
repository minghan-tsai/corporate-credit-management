CREATE TABLE drawdowns (
    id BIGSERIAL PRIMARY KEY,

    credit_limit_id BIGINT NOT NULL,

    amount NUMERIC(19, 2) NOT NULL,

    created_by BIGINT NOT NULL,

    created_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_drawdowns_credit_limit
        FOREIGN KEY (credit_limit_id)
        REFERENCES credit_limits(id),

    CONSTRAINT fk_drawdowns_created_by
        FOREIGN KEY (created_by)
        REFERENCES app_user(id)
);

CREATE INDEX idx_drawdowns_credit_limit_id
    ON drawdowns(credit_limit_id);

CREATE INDEX idx_drawdowns_created_by
    ON drawdowns(created_by);
