package com.vladmihalcea.book.high_performance_java_persistence.jdbc.batch;

import com.vladmihalcea.book.high_performance_java_persistence.jdbc.batch.providers.BatchEntityProvider;
import com.vladmihalcea.hibernate.masterclass.laboratory.util.AbstractOracleXEIntegrationTest;
import oracle.jdbc.pool.OracleDataSource;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;

/**
 * BatchStatementTest - Test batching with Statements
 *
 * @author Vlad Mihalcea
 */
public class OracleBatchStatementTest extends AbstractOracleXEIntegrationTest {

    public static final String INSERT_POST = "insert into Post (title, version, id) values ('Post no. %1$d', 0, %1$d)";

    public static final String INSERT_POST_COMMENT = "insert into PostComment (post_id, review, version, id) values (%1$d, 'Post comment %2$d', 0, %2$d)";

    private BatchEntityProvider entityProvider = new BatchEntityProvider();

    @Override
    protected DataSourceProvider getDataSourceProvider() {
        return new OracleDataSourceProvider() {
            @Override
            public DataSource dataSource() {
                OracleDataSource dataSource = (OracleDataSource) super.dataSource();
                try {
                    Properties connectionProperties = dataSource.getConnectionProperties();
                    if(connectionProperties == null) {
                        connectionProperties = new Properties();
                    }
                    connectionProperties.put("defaultExecuteBatch", 30);
                    dataSource.setConnectionProperties(connectionProperties);
                } catch (SQLException e) {
                    fail(e.getMessage());
                }
                return dataSource;
            }
        };
    }

    @Override
    protected Class<?>[] entities() {
        return entityProvider.entities();
    }

    @Test
    public void testInsert() {
        LOGGER.info("Test batch insert");
        long startNanos = System.nanoTime();
        doInConnection(connection -> {
            try (Statement statement = connection.createStatement()) {
                int postCount = getPostCount();
                int postCommentCount = getPostCommentCount();

                for(int i = 0; i < postCount; i++) {
                    statement.executeUpdate(String.format(INSERT_POST, i));
                    for(int j = 0; j < postCommentCount; j++) {
                        statement.executeUpdate(String.format(INSERT_POST_COMMENT, i, (postCommentCount * i) + j));
                    }
                }
            } catch (SQLException e) {
                fail(e.getMessage());
            }
        });
        LOGGER.info("{}.testInsert for {} took {} millis",
                getClass().getSimpleName(),
                getDataSourceProvider().getClass().getSimpleName(),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
    }

    protected int getPostCount() {
        return 1000;
    }

    protected int getPostCommentCount() {
        return 5;
    }
}
