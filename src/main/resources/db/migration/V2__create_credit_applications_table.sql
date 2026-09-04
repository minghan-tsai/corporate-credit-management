CREATE TABLE credit_applications (
    id BIGSERIAL PRIMARY KEY,

    company_id BIGINT NOT NULL,

    requested_amount NUMERIC(19, 2) NOT NULL,

    purpose VARCHAR(500) NOT NULL,

    status VARCHAR(30) NOT NULL,

    created_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_credit_applications_company
        FOREIGN KEY (company_id)
        REFERENCES company(id)
);