-- DROP DATABASE IF EXISTS audio;
CREATE DATABASE audio
    WITH
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'English_United States.utf8'
    LC_CTYPE = 'English_United States.utf8'
    LOCALE_PROVIDER = 'libc'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1
    IS_TEMPLATE = False;