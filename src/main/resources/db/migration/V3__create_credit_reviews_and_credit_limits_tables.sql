CREATE TABLE credit_reviews (
    id BIGSERIAL PRIMARY KEY,

    application_id BIGINT NOT NULL,

    decision VARCHAR(30) NOT NULL,

    approved_amount NUMERIC(19, 2),

    comment VARCHAR(255),

    reviewed_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_credit_reviews_application
        FOREIGN KEY (application_id)
        REFERENCES credit_applications(id)
);

CREATE TABLE credit_limits (
    id BIGSERIAL PRIMARY KEY,

    application_id BIGINT NOT NULL UNIQUE,

    limit_amount NUMERIC(19, 2) NOT NULL,

    available_amount NUMERIC(19, 2) NOT NULL,

    created_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_credit_limits_application
        FOREIGN KEY (application_id)
        REFERENCES credit_applications(id)
);
