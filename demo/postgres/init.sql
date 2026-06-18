-- Synthetic seed data for the Phileas Trino connector demo.
-- Every value below is fabricated for demonstration. There is no real PII here.

CREATE TABLE customers (
    id          INT PRIMARY KEY,
    full_name   VARCHAR(128),
    email       VARCHAR(128),
    ssn         VARCHAR(16),
    phone       VARCHAR(24)
);

INSERT INTO customers (id, full_name, email, ssn, phone) VALUES
    (1, 'Maria Gonzalez',  'maria.gonzalez@example.com', '123-45-6789', '(415) 555-0142'),
    (2, 'Toni Levine',     'toni.levine@example.org',    '987-65-4321', '415-555-0198'),
    (3, 'David Okafor',    'd.okafor@example.net',       '456-12-7890', '(212) 555-0173'),
    (4, 'Priya Nair',      'priya.nair@example.com',     '321-54-9876', '212-555-0150'),
    (5, 'James Whitfield', 'jwhitfield@example.org',     '654-32-1098', '(650) 555-0119');

CREATE TABLE support_transcripts (
    id          INT PRIMARY KEY,
    customer_id INT,
    transcript  VARCHAR(512)
);

INSERT INTO support_transcripts (id, customer_id, transcript) VALUES
    (1, 1, 'Caller Maria Gonzalez verified her identity with SSN 123-45-6789 and asked us to update the email on file to maria.gonzalez@example.com.'),
    (2, 2, 'Toni Levine called from (415) 555-0198 about a duplicate charge. Confirmed the card on file ending in 4242 4424 4242 4242.'),
    (3, 3, 'David Okafor requested a callback at (212) 555-0173 regarding his account; emailed a summary to d.okafor@example.net.'),
    (4, 4, 'Priya Nair reported she could not log in. We reset the account tied to priya.nair@example.com and confirmed her SSN 321-54-9876.'),
    (5, 5, 'James Whitfield asked to close the account; verification reference 654-32-1098, contact number (650) 555-0119.');
