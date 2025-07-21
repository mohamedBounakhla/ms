-- V1__Create_order_tables.sql
-- Initial migration for Order domain tables

-- Create ENUM types first
CREATE TYPE order_status_enum AS ENUM ('PENDING', 'PARTIAL', 'FILLED', 'CANCELLED');
CREATE TYPE currency_enum AS ENUM ('USD', 'EUR', 'GBP', 'JPY', 'BTC', 'ETH');

-- Buy Orders Table
CREATE TABLE buy_orders (
                            id VARCHAR(50) PRIMARY KEY,
                            symbol_code VARCHAR(20) NOT NULL,
                            symbol_name VARCHAR(100) NOT NULL,
                            price DECIMAL(19,8) NOT NULL CHECK (price > 0),
                            currency currency_enum NOT NULL,
                            quantity DECIMAL(19,8) NOT NULL CHECK (quantity > 0),
                            status order_status_enum NOT NULL DEFAULT 'PENDING',
                            executed_quantity DECIMAL(19,8) NOT NULL DEFAULT 0 CHECK (executed_quantity >= 0),
                            created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                            updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    -- Constraints
                            CONSTRAINT chk_buy_executed_quantity CHECK (executed_quantity <= quantity)
);

-- Sell Orders Table
CREATE TABLE sell_orders (
                             id VARCHAR(50) PRIMARY KEY,
                             symbol_code VARCHAR(20) NOT NULL,
                             symbol_name VARCHAR(100) NOT NULL,
                             price DECIMAL(19,8) NOT NULL CHECK (price > 0),
                             currency currency_enum NOT NULL,
                             quantity DECIMAL(19,8) NOT NULL CHECK (quantity > 0),
                             status order_status_enum NOT NULL DEFAULT 'PENDING',
                             executed_quantity DECIMAL(19,8) NOT NULL DEFAULT 0 CHECK (executed_quantity >= 0),
                             created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                             updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    -- Constraints
                             CONSTRAINT chk_sell_executed_quantity CHECK (executed_quantity <= quantity)
);

-- Transactions Table
CREATE TABLE transactions (
                              id VARCHAR(50) PRIMARY KEY,
                              symbol_code VARCHAR(20) NOT NULL,
                              symbol_name VARCHAR(100) NOT NULL,
                              buy_order_id VARCHAR(50) NOT NULL,
                              sell_order_id VARCHAR(50) NOT NULL,
                              price DECIMAL(19,8) NOT NULL CHECK (price > 0),
                              currency currency_enum NOT NULL,
                              quantity DECIMAL(19,8) NOT NULL CHECK (quantity > 0),
                              total_value DECIMAL(19,8) NOT NULL CHECK (total_value > 0),
                              created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    -- Foreign key constraints will be added when we have proper referential integrity
    -- For now, we keep them as simple varchar references to match your domain design
                              CONSTRAINT chk_transaction_total_value CHECK (total_value = price * quantity)
);

-- Create indexes for performance
CREATE INDEX idx_buy_orders_symbol_code ON buy_orders(symbol_code);
CREATE INDEX idx_buy_orders_status ON buy_orders(status);
CREATE INDEX idx_buy_orders_created_at ON buy_orders(created_at);
CREATE INDEX idx_buy_orders_symbol_status ON buy_orders(symbol_code, status);

CREATE INDEX idx_sell_orders_symbol_code ON sell_orders(symbol_code);
CREATE INDEX idx_sell_orders_status ON sell_orders(status);
CREATE INDEX idx_sell_orders_created_at ON sell_orders(created_at);
CREATE INDEX idx_sell_orders_symbol_status ON sell_orders(symbol_code, status);

CREATE INDEX idx_transactions_symbol_code ON transactions(symbol_code);
CREATE INDEX idx_transactions_buy_order_id ON transactions(buy_order_id);
CREATE INDEX idx_transactions_sell_order_id ON transactions(sell_order_id);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);

-- Add comments for documentation
COMMENT ON TABLE buy_orders IS 'Buy orders for trading system';
COMMENT ON TABLE sell_orders IS 'Sell orders for trading system';
COMMENT ON TABLE transactions IS 'Executed transactions between buy and sell orders';

COMMENT ON COLUMN buy_orders.executed_quantity IS 'Amount of the order that has been executed';
COMMENT ON COLUMN sell_orders.executed_quantity IS 'Amount of the order that has been executed';
COMMENT ON COLUMN transactions.total_value IS 'Total transaction value (price * quantity)';