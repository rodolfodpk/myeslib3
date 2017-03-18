
create table idempotency (
    entity_id char(36) NOT NULL,
    entity_key char(36) NOT NULL,
    datetime timestamp NOT NULL default CURRENT_TIMESTAMP,
    PRIMARY KEY (entity_id, entity_key)) ;


drop table if exists stomer_ar ;

// customer

create table
	customer_ar(id varchar(36) not null,
					  version number(38),
				      last_update timestamp,
					  constraint aggregate_pk primary key (id)) ;

drop table if exists customer_uow ;

create table
	customer_uow (id varchar(36) not null,
				        version number(38),
						uow_data json not null,
				        seq_number number(38),
				        inserted_on timestamp,
				        constraint uow_pk primary key (id, version));

drop sequence if exists seq_customer_uow ;

create sequence seq_customer_uow ;
