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
    total DECIMAL(10, 2),
    synced_status BOOLEAN DEFAULT FALSE
);

INSERT INTO sales (date, region, product, qty, cost, amt, tax, total)
VALUES
('2025-03-01', 'North', 'Laptop', 3, 500.00, 1500.00, 120.00, 1620.00),
('2025-03-02', 'South', 'Smartphone', 5, 300.00, 1500.00, 120.00, 1620.00),
('2025-03-03', 'East', 'Tablet', 2, 200.00, 400.00, 32.00, 432.00),
('2025-03-04', 'West', 'Headphones', 10, 50.00, 500.00, 40.00, 540.00),
('2025-03-05', 'Central', 'Monitor', 4, 150.00, 600.00, 48.00, 648.00);