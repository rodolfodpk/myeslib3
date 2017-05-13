# TODO
# Ja que nao vai precisar mais de 1 par de tabelas p/ aggregate root, talvez seja possivelsubstituir o jdbi pelo JOOQ

DROP TABLE if exists idempotency ;

CREATE TABLE idempotency (
    slot_name VARCHAR(36) NOT NULL,
    slot_id VARCHAR(36) NOT NULL,
    inserted_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (slot_name, slot_id)
    )
    PARTITION BY KEY(slot_name)
    ;

DROP TABLE if exists aggregate_roots ;

CREATE TABLE aggregate_roots (
    ar_name VARCHAR(36) NOT NULL,
    ar_id VARCHAR(36) NOT NULL,
    version BIGINT NOT NULL,
    last_updated_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (ar_name, ar_id)
    )
    PARTITION BY KEY(ar_name)
    ;

DROP TABLE if exists units_of_work ;

CREATE TABLE units_of_work (
	  uow_id VARCHAR(36) NOT NULL,
      uow_events JSON NOT NULL,
      uow_seq_number BIGINT AUTO_INCREMENT,
      cmd_id VARCHAR(36) NOT NULL,
      cmd_data json NOT NULL,
      ar_name VARCHAR(36) NOT NULL,
      ar_id VARCHAR(36) NOT NULL,
      version numeric,
      inserted_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (uow_id)
    )
    PARTITION BY KEY(ar_name)
    ;

CREATE INDEX ON units_of_work (uow_seq_number);

CREATE INDEX ON units_of_work (cmd_id);

CREATE INDEX ON units_of_work (ar_id);


