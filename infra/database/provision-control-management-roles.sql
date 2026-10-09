-- B02元数据及平台账号的固定函数owner，禁止应用登录身份继承或SET。
CREATE ROLE pet_control_manager_owner NOLOGIN NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE NOINHERIT;
GRANT pet_control_manager_owner TO pet_migrator WITH INHERIT FALSE, SET TRUE;
