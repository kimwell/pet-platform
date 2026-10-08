-- P05-05：管理员显式预配置，仅客户认证函数能力；不创建登录账号或秘密。
CREATE ROLE pet_customer_auth_owner NOLOGIN NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE NOINHERIT;
GRANT pet_customer_auth_owner TO pet_migrator WITH INHERIT FALSE, SET TRUE;
