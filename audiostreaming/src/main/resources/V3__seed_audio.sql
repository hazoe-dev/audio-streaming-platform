INSERT INTO users (id, created_at, email, password_hash, role)
VALUES (3,
        '2026-01-19 16:42:14.636025 +00:00',
        'user@test.com',
        '$2a$12$ZCOco8ZcrXdWoifRsq9xmO9i2c21zYIVJNpWm7e3IrEKLeyMyyfoe',
        'FREE');
INSERT INTO users (id, created_at, email, password_hash, role)
VALUES (4,
        '2026-01-19 16:43:09.693738 +00:00',
        'premium@test.com',
        '$2a$12$qqnPTol96skBbICsI/friOTV5LgeIVnm6/51DZpy.nKTr3yGhgarm',
        'FREE');


insert into audio (id,title, description, duration_seconds,
                   audio_path, cover_path, is_premium, owner_id,
                   created_at)
values (1,
        'Mindful Focus',
        'Guided meditation',
        1800,
        'audio/2026/01/mindful_focus.mp3',
        'cover/2026/01/mindful_focus.png',
        false,
        1,
        '2026-01-19 16:42:14.636025 +00:00'
       );