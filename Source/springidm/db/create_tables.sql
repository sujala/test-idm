create table `users` (
	username varchar(255) primary key, 
	accountid varchar(50), 
	password varchar(255), 
	firstname varchar(255), 
	lastname varchar(255)
);

create table `providers`(
	id int(10) not null primary key auto_increment,
	name varchar(255) not null,
	oname varchar(255)
);
	
create table `consumers`(
	id int(20) not null primary key auto_increment,
	name varchar(255) not null,
	oname varchar(255),
	providerid int(10) not null,
	providername varchar(255),
	consumer_key varchar(255) not null, 
	consumer_secret varchar(255) not null,
	self_provider tinyint(1) default 0
);

create table `request_tokens`(
	token varchar(255) primary key,
	provider_id int(10) not null,
	provider_name varchar(255) not null,
	consumer_id int(10) not null,
	consumer_name varchar(255) not null,
	datecreated timestamp DEFAULT CURRENT_TIMESTAMP
);

create table `access_tokens`(
	token varchar(255) primary key,
	username varchar(255) not null,
	token_secret varchar(255) not null,
	refresh_token varchar(255) not null,
	expire_in int default 3600,
	datecreated timestamp DEFAULT CURRENT_TIMESTAMP
);