CREATE TABLE payment
(
    uu_id          VARCHAR(255) NOT NULL,
    booking_id     VARCHAR(255) NULL,
    amount         DOUBLE       NOT NULL,
    currency       VARCHAR(255) NULL,
    status         SMALLINT     NULL,
    failure_reason VARCHAR(255) NULL,
    timestamps     VARCHAR(255) NULL,
    CONSTRAINT pk_payment PRIMARY KEY (uu_id)
);
