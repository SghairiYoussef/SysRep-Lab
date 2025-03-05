CREATE DATABASE IF NOT EXISTS sales_db;

USE sales_db;

CREATE TABLE IF NOT EXISTS sales (
    id CHAR(36) PRIMARY KEY,
    date DATE,
    product VARCHAR(255),
    qty INT,
    cost DECIMAL(10, 2),
    amt DECIMAL(10, 2),
    tax DECIMAL(10, 2),
    total DECIMAL(10, 2),
);