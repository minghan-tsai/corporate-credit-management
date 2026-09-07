ALTER TABLE credit_applications
    ADD COLUMN created_by BIGINT;

ALTER TABLE credit_applications
    ADD CONSTRAINT fk_credit_applications_created_by
        FOREIGN KEY (created_by)
        REFERENCES app_user(id);
