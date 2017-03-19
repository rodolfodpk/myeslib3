
drop table if exists public.idempotency ;

create table public.idempotency (
    entity_id char(36) NOT NULL,
    entity_key char(36) NOT NULL,
    datetime timestamp NOT NULL default CURRENT_TIMESTAMP,
    PRIMARY KEY (entity_id, entity_key)) ;

drop table if exists public.customer_ar ;

create table
	public.customer_ar(id varchar(36) primary key not null,
			    version numeric,
				last_update timestamp) ;

drop table if exists public.customer_uow ;

create table
	public.customer_uow (id varchar(36) primary key not null,
                  version numeric,
				  uow_data json not null,
				  seq_number serial,
                  inserted_on timestamp);
