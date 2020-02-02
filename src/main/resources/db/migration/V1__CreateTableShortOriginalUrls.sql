CREATE TABLE short_original_urls(
    short_url varchar(10) PRIMARY KEY,
    original_url text UNIQUE
);