
create table idempotency (
    entity_id char(36) NOT NULL,
    entity_key char(36) NOT NULL,
    datetime timestamp NOT NULL default CURRENT_TIMESTAMP,
    PRIMARY KEY (entity_id, entity_key)) ;
