insert into sys_user
(id, username, password, nickname, email, role, status, create_time, update_time)
values
(1, 'admin', '$2a$10$JtOFioewneqMwmOJPoak8.lxg/a0sjIXl8seCyyLCUmeRYSngWq0y', 'admin', 'admin@example.com', 'admin', 1, now(), now())
on duplicate key update
password = values(password),
nickname = values(nickname),
email = values(email),
role = values(role),
status = values(status),
update_time = now();
