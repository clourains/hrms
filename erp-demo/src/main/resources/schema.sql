CREATE TABLE IF NOT EXISTS erp_outbound (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    outbound_no    VARCHAR(64)  NOT NULL,
    carrier        VARCHAR(64),
    tracking_no    VARCHAR(100) NOT NULL,
    transport_type VARCHAR(32),
    port_from      VARCHAR(64),
    port_to        VARCHAR(64),
    dest_country   VARCHAR(64),
    order_no       VARCHAR(64),
    ship_date      DATE,
    status         VARCHAR(16) DEFAULT 'WAIT',
    create_time    TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_erp_outbound_no UNIQUE (outbound_no),
    CONSTRAINT uk_erp_outbound_tracking UNIQUE (tracking_no)
);

CREATE TABLE IF NOT EXISTS erp_import_history (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_name     VARCHAR(255) NOT NULL,
    rows_total    INT DEFAULT 0,
    rows_imported INT DEFAULT 0,
    rows_skipped  INT DEFAULT 0,
    status        VARCHAR(16) NOT NULL,
    message       VARCHAR(500),
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS erp_shipment_push_outbox (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    outbound_id   BIGINT,
    tracking_no   VARCHAR(100) NOT NULL,
    payload       TEXT,
    status        VARCHAR(16) DEFAULT 'PENDING',
    attempts      INT         DEFAULT 0,
    last_error    VARCHAR(500),
    next_retry_at TIMESTAMP,
    create_time   TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_erp_outbox_tracking UNIQUE (tracking_no)
);
