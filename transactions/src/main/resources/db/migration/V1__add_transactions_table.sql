CREATE TABLE transactions
(
    id             UUID PRIMARY KEY         DEFAULT uuidv7(),
    broker_id      UUID           NOT NULL,
    portfolio_id   UUID           NOT NULL,
    asset_id       VARCHAR(20)    NOT NULL,
    operation_date DATE           NOT NULL,
    operation_type VARCHAR(10)    NOT NULL CHECK (operation_type IN ('BUY', 'SELL')),
    quantity       INTEGER        NOT NULL CHECK (quantity > 0),
    unit_price     DECIMAL(10,4)  NOT NULL CHECK (unit_price >= 0),
    fees           DECIMAL(10,2)  NOT NULL  DEFAULT 0,
    created_at     TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);