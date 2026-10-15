package com.medflow.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedflowSystemHealthIndicatorTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private Statement statement;

    @InjectMocks
    private MedflowSystemHealthIndicator healthIndicator;

    @Test
    @DisplayName("HealthIndicator: deve retornar UP com detalhes quando o banco de dados responder com sucesso")
    void testHealthUpQuandoBancoDisponivel() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute("SELECT 1")).thenReturn(true);

        Health health = healthIndicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals("PostgreSQL Conectado e Operacional", health.getDetails().get("database"));
        assertEquals("MedFlow Core Online", health.getDetails().get("status"));
        assertNotNull(health.getDetails().get("jvm.memory.freeMB"));
    }

    @Test
    @DisplayName("HealthIndicator: deve retornar DOWN quando houver falha de conexão com o banco")
    void testHealthDownQuandoFalhaBanco() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection refused"));

        Health health = healthIndicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("Falha de conectividade com PostgreSQL", health.getDetails().get("database"));
        assertEquals("Connection refused", health.getDetails().get("error"));
    }
}
