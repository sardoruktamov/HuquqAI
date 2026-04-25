CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

insert into profile(id, name, username, password, status, visible, created_date)
values (1,
        'Admin','uktamov9198@mail.ru',
        '$2a$10$05YK7WHFlcEUVVR.qWDGFuOFDaRxtkfAjRGuxJhRaE5p7daWeccMW',
        'ACTIVE',true,now());

select setval('profile_id_seq', max(id)) from profile;

insert into profile_role(profile_id, roles, created_date)
values (1,'ROLE_USER',now()),
       (1,'ROLE_ADMIN',now()),
       (1,'ROLE_SUPERADMIN',now());