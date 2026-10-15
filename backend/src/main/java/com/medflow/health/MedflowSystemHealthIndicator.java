package com.medflow.health;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Indicador avançado de integridade (Health Check) do MedFlow.
 * Verifica a conectividade com o banco de dados PostgreSQL e métricas de memória da JVM.
 */
@Component
public class MedflowSystemHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    public MedflowSystemHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("SELECT 1");

            long freeMemory = Runtime.getRuntime().freeMemory();
            long totalMemory = Runtime.getRuntime().totalMemory();
            long maxMemory = Runtime.getRuntime().maxMemory();

            return Health.up()
                    .withDetail("database", "PostgreSQL Conectado e Operacional")
                    .withDetail("jvm.memory.freeMB", freeMemory / (1024 * 1024))
                    .withDetail("jvm.memory.totalMB", totalMemory / (1024 * 1024))
                    .withDetail("jvm.memory.maxMB", maxMemory / (1024 * 1024))
                    .withDetail("status", "MedFlow Core Online")
                    .build();
        } catch (Exception ex) {
            return Health.down()
                    .withDetail("database", "Falha de conectividade com PostgreSQL")
                    .withDetail("error", ex.getMessage())
                    .build();
        }
    }
}
