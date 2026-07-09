CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(120) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE books (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    author VARCHAR(150) NOT NULL,
    publisher VARCHAR(150),
    category VARCHAR(20) NOT NULL,
    isbn VARCHAR(20),
    price NUMERIC(10, 2) NOT NULL CHECK (price > 0),
    stock_quantity INTEGER NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    cover_url TEXT,
    synopsis TEXT,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_books_title ON books (lower(title));
CREATE INDEX idx_books_author ON books (lower(author));
CREATE INDEX idx_books_category ON books (category);
CREATE INDEX idx_books_active ON books (active);

CREATE TABLE stock_movements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    book_id UUID NOT NULL REFERENCES books (id),
    type VARCHAR(10) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    note VARCHAR(255),
    created_by UUID REFERENCES users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_stock_movements_book_id ON stock_movements (book_id);
