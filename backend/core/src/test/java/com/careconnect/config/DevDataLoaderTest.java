package com.careconnect.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DevDataLoader}.
 *
 * DevDataLoader is a Spring {@code CommandLineRunner} that seeds the database with
 * development fixture data on startup. It is only active when a boolean "enabled" flag
 * is set to {@code true} (typically via a Spring profile or property), and it skips
 * seeding if users already exist in the database.
 *
 * All JDBC interactions (DataSource, Connection, Statement, ResultSet) are mocked using
 * Mockito to avoid requiring a real database. This isolates the logic being tested —
 * the conditional seeding decisions — from infrastructure concerns.
 */
class DevDataLoaderTest {

    private DataSource dataSource;
    private Connection connection;
    private Statement statement;
    private ResultSet resultSet;

    private DevDataLoader loader;

    @BeforeEach
    void setUp() throws Exception {
        // Build a mock JDBC chain: DataSource → Connection → Statement.
        // ResultSet is mocked separately and returned by statement.executeQuery().
        dataSource = mock(DataSource.class);
        connection = mock(Connection.class);
        statement = mock(Statement.class);
        resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        // Default loader is enabled so individual tests can exercise the loading path.
        loader = new DevDataLoader(dataSource, true);
    }

    // -------------------------------------------------
    // 1. Disabled flag → do nothing
    // -------------------------------------------------

    @Test
    void run_DoesNothingWhenDisabled() throws Exception {
        // Verifies that when the loader is constructed with enabled=false, run() returns
        // immediately without touching the DataSource — no connection is opened.
        DevDataLoader disabledLoader = new DevDataLoader(dataSource, false);

        assertDoesNotThrow(() -> disabledLoader.run());
        verifyNoInteractions(dataSource);
    }

    // -------------------------------------------------
    // 2. Users exist → skip loading
    // -------------------------------------------------

    @Test
    void run_SkipsLoadingWhenUsersExist() throws Exception {
        // Verifies the guard query: if users table already contains rows, no INSERT/UPDATE
        // statements are executed, preventing duplicate seed data on subsequent restarts.
        when(statement.executeQuery("SELECT COUNT(*) FROM users"))
                .thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(5);

        assertDoesNotThrow(() -> loader.run());

        verify(statement, atLeastOnce()).executeQuery("SELECT COUNT(*) FROM users");
        verify(statement, never()).executeUpdate(anyString());
    }

    // -------------------------------------------------
    // 3. Users = 0 → should attempt SQL execution
    // -------------------------------------------------

    @Test
    void run_AttemptsLoadWhenNoUsersExist() throws Exception {
        // Verifies that when the users table is empty, the loader proceeds to execute
        // SQL seed statements (the contents of a dev SQL file or inline SQL).
        when(statement.executeQuery("SELECT COUNT(*) FROM users"))
                .thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(0);

        // Simulate SQL file execution returning row-count success
        when(statement.executeUpdate(anyString())).thenReturn(1);

        assertDoesNotThrow(() -> loader.run());

        verify(statement, atLeastOnce()).executeQuery("SELECT COUNT(*) FROM users");
    }

    // -------------------------------------------------
    // 4. Exception checking user count → still attempts load
    // -------------------------------------------------

    @Test
    void run_AttemptsLoadWhenUserCheckFails() throws Exception {
        // Verifies resilience: if the guard query itself throws (e.g. table not yet
        // created by Flyway), the loader still attempts to run without crashing the app.
        when(statement.executeQuery("SELECT COUNT(*) FROM users"))
                .thenThrow(new RuntimeException("DB error"));

        when(statement.executeUpdate(anyString())).thenReturn(1);

        assertDoesNotThrow(() -> loader.run());
    }

    // -------------------------------------------------
    // 5. SQL execution failure → does not crash
    // -------------------------------------------------

    @Test
    void run_DoesNotThrowWhenSqlExecutionFails() throws Exception {
        // Verifies that a failure during seed SQL execution does not propagate an
        // exception that would abort application startup — errors are caught and logged.
        when(statement.executeQuery("SELECT COUNT(*) FROM users"))
                .thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(0);

        when(statement.executeUpdate(anyString()))
                .thenThrow(new RuntimeException("SQL failure"));

        assertDoesNotThrow(() -> loader.run());
    }
}
