package com.vladmihalcea.book.high_performance_java_persistence.jdbc.connection;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import com.vladmihalcea.book.high_performance_java_persistence.jdbc.batch.providers.SequenceBatchEntityProvider;
import com.vladmihalcea.hibernate.masterclass.laboratory.util.AbstractTest;
import com.vladmihalcea.hibernate.masterclass.laboratory.util.DataSourceProviderIntegrationTest;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * <code>ConnectionPoolCallTest</code> - Checks how connection pool decreases latency
 *
 * @author Vlad Mihalcea
 */
@Ignore
public class ConnectionPoolCallTest extends DataSourceProviderIntegrationTest {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private MetricRegistry metricRegistry = new MetricRegistry();

    private Timer timer = metricRegistry.timer("callTimer");

    private Slf4jReporter logReporter = Slf4jReporter
            .forRegistry(metricRegistry)
            .outputTo(LOGGER)
            .build();

    private int callCount = 1000;

    public ConnectionPoolCallTest(DataSourceProvider dataSourceProvider) {
        super(dataSourceProvider);
    }

    @Override
    protected Class<?>[] entities() {
        return new Class[]{};
    }

    @Test
    public void testNoPooling() throws SQLException {
        LOGGER.info("Test without pooling for {}", getDataSourceProvider().database());
        test(getDataSourceProvider().dataSource());
    }

    @Test
    public void testPooling() throws SQLException {
        LOGGER.info("Test with pooling for {}", getDataSourceProvider().database());
        test(poolingDataSource());
    }

    private void test(DataSource dataSource) throws SQLException {
        for (int i = 0; i < callCount; i++) {
            long startNanos = System.nanoTime();
            try (Connection connection = dataSource.getConnection()) {
            }
            timer.update(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
        }
        logReporter.report();
    }

    protected HikariDataSource poolingDataSource() {
        Properties properties = new Properties();
        properties.setProperty("dataSourceClassName", getDataSourceProvider().dataSourceClassName().getName());
        properties.put("dataSourceProperties", getDataSourceProvider().dataSourceProperties());
        properties.setProperty("minimumPoolSize", String.valueOf(1));
        properties.setProperty("maximumPoolSize", String.valueOf(3));
        properties.setProperty("connectionTimeout", String.valueOf(100));
        return new HikariDataSource(new HikariConfig(properties));
    }
}
