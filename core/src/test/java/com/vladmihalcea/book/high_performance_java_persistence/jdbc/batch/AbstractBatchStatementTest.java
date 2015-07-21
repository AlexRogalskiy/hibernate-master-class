package com.vladmihalcea.book.high_performance_java_persistence.jdbc.batch;

import com.vladmihalcea.hibernate.masterclass.laboratory.util.DataSourceProviderIntegrationTest;
import org.junit.Test;

import javax.persistence.*;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.fail;

/**
 * AbstractBatchStatementTest - Base class for testing JDBC Statement batching
 *
 * @author Vlad Mihalcea
 */
public abstract class AbstractBatchStatementTest extends DataSourceProviderIntegrationTest {

    public static final String INSERT_POST = "insert into Post (title, version, id) values ('Post no. %1$d', 0, %1$d)";

    public static final String INSERT_POST_COMMENT = "insert into PostComment (post_id, review, version, id) values (%1$d, 'Post comment %2$d', 0, %2$d)";

    private final BatchEntityProvider entityProvider = new BatchEntityProvider();

    public AbstractBatchStatementTest(DataSourceProvider dataSourceProvider) {
        super(dataSourceProvider);
    }

    @Override
    protected Class<?>[] entities() {
        return entityProvider.entities();
    }

    @Test
    public void testInsert() {
        LOGGER.info("Test batch insert");
        AtomicInteger statementCount = new AtomicInteger();
        long startNanos = System.nanoTime();
        doInConnection(connection -> {
            try (Statement statement = connection.createStatement()) {
                int postCount = getPostCount();
                int postCommentCount = getPostCommentCount();

                for(int i = 0; i < postCount; i++) {
                    executeStatement(statement, String.format(INSERT_POST, i), statementCount);
                    if (insertComments()) {
                        for(int j = 0; j < postCommentCount; j++) {
                            executeStatement(statement, String.format(INSERT_POST_COMMENT, i, (postCommentCount * i) + j), statementCount);
                        }
                    }
                }
                onEnd(statement);
            } catch (SQLException e) {
                fail(e.getMessage());
            }
        });
        LOGGER.info("{}.testInsert for {} took {} millis",
                getClass().getSimpleName(),
                getDataSourceProvider().getClass().getSimpleName(),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
    }

    protected abstract void onFlush(Statement statement) throws SQLException;

    private void executeStatement(Statement statement, String dml, AtomicInteger statementCount) throws SQLException {
        onStatement(statement, dml);
        int count = statementCount.incrementAndGet();
        if(count % getBatchSize() == 0) {
            onFlush(statement);
        }
    }

    protected abstract void onStatement(Statement statement, String dml) throws SQLException;

    protected abstract void onEnd(Statement statement) throws SQLException;

    protected int getPostCount() {
        return 5000;
    }

    protected int getPostCommentCount() {
        return 5;
    }

    protected int getBatchSize() {
        return 50;
    }

    protected boolean insertComments() {
        return false;
    }
}
