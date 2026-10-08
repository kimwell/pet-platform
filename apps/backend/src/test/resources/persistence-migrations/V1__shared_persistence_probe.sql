-- 真实 PostgreSQL 技术夹具，仅在 src/test；不是正式业务表。
CREATE TABLE persistence_probe (
    id uuid PRIMARY KEY,
    created_at timestamp(3) with time zone NOT NULL,
    updated_at timestamp(3) with time zone NOT NULL,
    code varchar(255) NOT NULL UNIQUE,
    display_name varchar(255),
    tag varchar(255) NOT NULL
);
