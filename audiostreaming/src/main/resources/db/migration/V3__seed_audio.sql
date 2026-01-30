INSERT INTO users (created_at, email, password_hash, role)
VALUES (
        '2026-01-19 16:42:14.636025 +00:00',
        'admin@test.com',
        '$2a$12$e.6JniMBskihENyg/p8DQeQ7GmiSOi3hoMDqmLP3BQU.KkFk2Jbea',--admin@123456
        'ADMIN'),
       (
        '2026-01-19 16:42:14.636025 +00:00',
        'user@test.com',
        '$2a$12$ZCOco8ZcrXdWoifRsq9xmO9i2c21zYIVJNpWm7e3IrEKLeyMyyfoe',--user@123456
        'FREE'),
       (
        '2026-01-19 16:43:09.693738 +00:00',
        'premium@test.com',
        '$2a$12$qqnPTol96skBbICsI/friOTV5LgeIVnm6/51DZpy.nKTr3yGhgarm',--premium@123456
        'PREMIUM');


insert into audio ( title, description, duration_seconds,
                   audio_path, content_type, cover_path, is_premium,
                   owner_id, created_at)
values (
        'Mindful Focus',
        'Guided meditation',
        1800,
        'audio/2026/01/mindful_focus.mp3',
        'audio/mpeg',
        'cover/2026/01/mindful_focus.png',
        false,
        1,
        '2026-01-19 16:42:14.636025 +00:00'),
       (
        'Brain On',
        'Practice your brain on work',
        32,
        'audio/2026/01/brain_on.mp3',
        'audio/mpeg',
        'cover/2026/01/brain_on.png',
        true,
        1,
        '2026-01-20 16:42:14.636025 +00:00');