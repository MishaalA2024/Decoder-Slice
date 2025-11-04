package com.decoder.config;

import org.springframework.context.annotation.Configuration;

/**
 * Database configuration for SQLite.
 * In production, this would be configured for PostgreSQL.
 * SQLite dialect is configured in application.yml.
 */
@Configuration
public class DatabaseConfig {
    // SQLite dialect configuration is handled by Hibernate automatically
    // when using sqlite-jdbc driver and custom SQLiteDialect class
}
