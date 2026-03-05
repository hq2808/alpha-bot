-- Fix sequence after manual ID insert in V12
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
