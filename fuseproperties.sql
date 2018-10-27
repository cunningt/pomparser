create table fuseproperties(
  project varchar(64) not null,
  branch varchar(64) not null,
  pom varchar(256) not null,
  propertyname varchar(256) not null,
  propertyvalue varchar(3500) not null   
);
alter table fuseproperties add index (propertyname);
