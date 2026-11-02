-- Тестовые данные для локальной разработки

INSERT INTO department(name)
VALUES ('Кафедра информационных технологий'),
       ('Кафедра математики');

-- password_hash пока заглушка: войти с ней через security нельзя
INSERT INTO users(login, password_hash, name, last_name, patronymic, role_id, department_id)
VALUES
    ('teacher1', 'CHANGE_ME', 'Анна',   'Иванова',  'Петровна',
        (SELECT id FROM role WHERE name_role = 'TEACHER'),
        (SELECT id FROM department WHERE name = 'Кафедра информационных технологий')),
    ('teacher2', 'CHANGE_ME', 'Сергей', 'Петров',   'Иванович',
        (SELECT id FROM role WHERE name_role = 'TEACHER'),
        (SELECT id FROM department WHERE name = 'Кафедра информационных технологий')),
    ('head1',    'CHANGE_ME', 'Олег',   'Смирнов',  'Николаевич',
        (SELECT id FROM role WHERE name_role = 'DEPARTMENT_HEAD'),
        (SELECT id FROM department WHERE name = 'Кафедра информационных технологий')),
    ('staff1',   'CHANGE_ME', 'Мария',  'Кузнецова','Андреевна',
        (SELECT id FROM role WHERE name_role = 'DEPARTMENT_STAFF'),
        (SELECT id FROM department WHERE name = 'Кафедра информационных технологий')),
    ('duty1',    'CHANGE_ME', 'Дмитрий','Соколов',  'Алексеевич',
        (SELECT id FROM role WHERE name_role = 'DUTY_OFFICER'), NULL),
    ('admin',    'CHANGE_ME', 'Админ',  'Админов',  NULL,
        (SELECT id FROM role WHERE name_role = 'ADMIN'), NULL);

INSERT INTO audience(number)
VALUES ('Д-401'), ('Д-215'), ('Т-103');

INSERT INTO service_catalog(name_service, description, deadline)
VALUES
    ('Установка ПО',        'Установка и настройка программного обеспечения', 48),
    ('Замена картриджа',    'Замена картриджа в принтере',                    24),
    ('Ремонт проектора',    'Диагностика и ремонт проектора',                 72),
    ('Настройка Wi-Fi',     'Подключение и настройка сети',                   24);

INSERT INTO request(user_id, audience_id, service_id, status_id, description,
                    approver_id, executor_id, created_at, is_approved, reject_reason)
VALUES
    -- 1: только создана, ждёт заведующего
    ((SELECT id FROM users WHERE login = 'teacher1'),
     (SELECT id FROM audience WHERE number = 'Д-401'),
     (SELECT id FROM service_catalog WHERE name_service = 'Установка ПО'),
     (SELECT id FROM request_status WHERE name = 'PENDING_HEAD'),
     'Нужен Blender на 12 рабочих станций',
     NULL, NULL, now() - interval '1 hour', NULL, NULL),

    -- 2: в работе
    ((SELECT id FROM users WHERE login = 'teacher1'),
     (SELECT id FROM audience WHERE number = 'Д-215'),
     (SELECT id FROM service_catalog WHERE name_service = 'Замена картриджа'),
     (SELECT id FROM request_status WHERE name = 'IN_PROGRESS'),
     'Закончился тонер',
     (SELECT id FROM users WHERE login = 'head1'),
     (SELECT id FROM users WHERE login = 'duty1'),
     now() - interval '2 days', true, NULL),

    -- 3: отклонена (по CHECK причина обязательна)
    ((SELECT id FROM users WHERE login = 'teacher1'),
     (SELECT id FROM audience WHERE number = 'Д-401'),
     (SELECT id FROM service_catalog WHERE name_service = 'Ремонт проектора'),
     (SELECT id FROM request_status WHERE name = 'REJECTED'),
     'Проектор не включается',
     (SELECT id FROM users WHERE login = 'head1'),
     NULL, now() - interval '3 days', false, 'Проектор списан, нужна замена через закупку'),

    -- 4: назначена исполнителю
    ((SELECT id FROM users WHERE login = 'teacher2'),
     (SELECT id FROM audience WHERE number = 'Т-103'),
     (SELECT id FROM service_catalog WHERE name_service = 'Настройка Wi-Fi'),
     (SELECT id FROM request_status WHERE name = 'ASSIGNED'),
     NULL,
     (SELECT id FROM users WHERE login = 'head1'),
     (SELECT id FROM users WHERE login = 'duty1'),
     now() - interval '1 day', true, NULL),

    -- 5: выполнена
    ((SELECT id FROM users WHERE login = 'teacher2'),
     (SELECT id FROM audience WHERE number = 'Д-215'),
     (SELECT id FROM service_catalog WHERE name_service = 'Установка ПО'),
     (SELECT id FROM request_status WHERE name = 'DONE'),
     'Установить Python',
     (SELECT id FROM users WHERE login = 'head1'),
     (SELECT id FROM users WHERE login = 'duty1'),
     now() - interval '7 days', true, NULL);
