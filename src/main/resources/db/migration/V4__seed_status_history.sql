-- История статусов для пяти заявок из V2.
-- Каждая строка: какая заявка, из какого статуса в какой, кто перевёл, через сколько после создания.
INSERT INTO request_status_history(request_id, from_status_id, to_status_id, changed_by, changed_at, reason)
SELECT r.id, fs.id, ts.id, u.id, r.created_at + h.shift, h.reason
FROM (VALUES
    -- заявка 1: только создана
    ('teacher1','Д-401','Установка ПО',     NULL,                 'PENDING_HEAD',       'teacher1', INTERVAL '0',      NULL),

    -- заявка 2: дошла до IN_PROGRESS
    ('teacher1','Д-215','Замена картриджа', NULL,                 'PENDING_HEAD',       'teacher1', INTERVAL '0',      NULL),
    ('teacher1','Д-215','Замена картриджа', 'PENDING_HEAD',       'PENDING_DEPARTMENT', 'head1',    INTERVAL '3 hours', NULL),
    ('teacher1','Д-215','Замена картриджа', 'PENDING_DEPARTMENT', 'ASSIGNED',           'staff1',   INTERVAL '5 hours', NULL),
    ('teacher1','Д-215','Замена картриджа', 'ASSIGNED',           'IN_PROGRESS',        'duty1',    INTERVAL '1 day',   NULL),

    -- заявка 3: отклонена заведующим
    ('teacher1','Д-401','Ремонт проектора', NULL,                 'PENDING_HEAD',       'teacher1', INTERVAL '0',      NULL),
    ('teacher1','Д-401','Ремонт проектора', 'PENDING_HEAD',       'REJECTED',           'head1',    INTERVAL '2 hours', 'Проектор списан, нужна замена через закупку'),

    -- заявка 4: назначена исполнителю
    ('teacher2','Т-103','Настройка Wi-Fi',  NULL,                 'PENDING_HEAD',       'teacher2', INTERVAL '0',      NULL),
    ('teacher2','Т-103','Настройка Wi-Fi',  'PENDING_HEAD',       'PENDING_DEPARTMENT', 'head1',    INTERVAL '1 hour',  NULL),
    ('teacher2','Т-103','Настройка Wi-Fi',  'PENDING_DEPARTMENT', 'ASSIGNED',           'staff1',   INTERVAL '2 hours', NULL),

    -- заявка 5: выполнена
    ('teacher2','Д-215','Установка ПО',     NULL,                 'PENDING_HEAD',       'teacher2', INTERVAL '0',      NULL),
    ('teacher2','Д-215','Установка ПО',     'PENDING_HEAD',       'PENDING_DEPARTMENT', 'head1',    INTERVAL '2 hours', NULL),
    ('teacher2','Д-215','Установка ПО',     'PENDING_DEPARTMENT', 'ASSIGNED',           'staff1',   INTERVAL '4 hours', NULL),
    ('teacher2','Д-215','Установка ПО',     'ASSIGNED',           'IN_PROGRESS',        'duty1',    INTERVAL '1 day',   NULL),
    ('teacher2','Д-215','Установка ПО',     'IN_PROGRESS',        'DONE',               'duty1',    INTERVAL '3 days',  NULL)
) AS h(author_login, audience_number, service_name, from_status, to_status, by_login, shift, reason)
JOIN users a            ON a.login = h.author_login
JOIN audience au        ON au.number = h.audience_number
JOIN service_catalog sc ON sc.name_service = h.service_name
JOIN request r          ON r.user_id = a.id AND r.audience_id = au.id AND r.service_id = sc.id
LEFT JOIN request_status fs ON fs.name = h.from_status
JOIN request_status ts  ON ts.name = h.to_status
JOIN users u            ON u.login = h.by_login;
