CREATE DATABASE IF NOT EXISTS realestate;
USE realestate;

CREATE TABLE IF NOT EXISTS realtors (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS listings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    price DECIMAL(12, 2) NOT NULL,
    realtor_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (realtor_id) REFERENCES realtors(id)
    );

INSERT INTO realtors (username,password) VALUES
     ('alice', '$2b$10$zE/3gTznMJPWUpsUpU/LZud4eZ7vBC.NIVB./T2ZKHYX/moBUVjGW'), -- password=password123
     ('bob', '$2b$10$cNfbS5nfwk1RUHUjtxOfx.Qb0sR6rbdl2du3kljp2h69y1S2P5xPC'); -- password=hunter2

INSERT INTO listings (title, description, price, realtor_id) VALUES
     ('Cosy 2-bed in Melville', 'Close to shops and restaurants', 1250000.00, 1),
     ('Modern loft in Braamfontein', 'Open plan, great for students', 875000.00, 1),
     ('Family home in Randburg', 'Big garden, 3 bedrooms', 2100000.00, 2);