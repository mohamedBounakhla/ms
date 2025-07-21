-- V2__Create_user_tables.sql
-- User management tables for security domain

-- User table (MSUser entity)
CREATE TABLE msuser (
                        username VARCHAR(20) PRIMARY KEY,
                        userpassword VARCHAR(500) NOT NULL,
                        userrole VARCHAR(20) NOT NULL CHECK (userrole IN ('CUSTOMER', 'GUEST', 'ADMIN')),
                        created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_msuser_userrole ON msuser(userrole);
CREATE INDEX idx_msuser_created_at ON msuser(created_at);

-- Add comments
COMMENT ON TABLE msuser IS 'User accounts for the trading system';
COMMENT ON COLUMN msuser.username IS 'Unique username for login';
COMMENT ON COLUMN msuser.userpassword IS 'Encrypted password';
COMMENT ON COLUMN msuser.userrole IS 'User role: CUSTOMER, GUEST, or ADMIN';

-- Insert default admin user (password: admin123)
-- Note: This password is encoded using Spring Security's default encoder
INSERT INTO msuser (username, userpassword, userrole)
VALUES ('admin', '{bcrypt}$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8loxzOnY6s2NyOWV2C', 'ADMIN');

-- Insert test customer user (password: customer123)
INSERT INTO msuser (username, userpassword, userrole)
VALUES ('testuser', '{bcrypt}$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8loxzOnY6s2NyOWV2C', 'CUSTOMER');