CREATE DATABASE IF NOT EXISTS sales_db;

USE sales_db;

CREATE TABLE IF NOT EXISTS sales (
    id INT AUTO_INCREMENT PRIMARY KEY,
    date DATE,
    region VARCHAR(255),
    product VARCHAR(255),
    qty INT,
    cost DECIMAL(10, 2),
    amt DECIMAL(10, 2),
    tax DECIMAL(10, 2),
    total DECIMAL(10, 2)
);
