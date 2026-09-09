CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,

    actor_username VARCHAR(100) NOT NULL,

    action VARCHAR(50) NOT NULL,

    entity_type VARCHAR(50) NOT NULL,

    entity_id BIGINT NOT NULL,

    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_audit_logs_entity
    ON audit_logs(entity_type, entity_id);
