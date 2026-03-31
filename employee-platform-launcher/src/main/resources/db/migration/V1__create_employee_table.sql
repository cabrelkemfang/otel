CREATE TABLE t_employee
(
    employee_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name   VARCHAR(100)        NOT NULL,
    last_name    VARCHAR(100)        NOT NULL,
    email        VARCHAR(255)        NOT NULL UNIQUE,
    phone_number VARCHAR(20)         NOT NULL,
    department   VARCHAR(50)         NOT NULL,
    created_at   DATETIME            NOT NULL
);
