package com.vladmihalcea.book.high_performance_java_persistence.jdbc.batch;

import com.vladmihalcea.book.high_performance_java_persistence.jdbc.batch.providers.BatchEntityProvider;
import com.vladmihalcea.hibernate.masterclass.laboratory.util.AbstractOracleXEIntegrationTest;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.pool.OracleDataSource;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * OracleImplicitStatementCacheTest - Test Oracle implicit Statement cache
 *
 * @author Vlad Mihalcea
 */
public class OracleImplicitStatementCacheTest extends AbstractOracleXEIntegrationTest {

    public static final String INSERT_POST = "insert into Post (title, version, id) values (?, ?, ?)";

    public static final String INSERT_POST_COMMENT = "insert into PostComment (post_id, review, version, id) values (?, ?, ?, ?)";

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
                    connectionProperties.put("oracle.jdbc.implicitStatementCacheSize", "5");
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

    @Override
    public void init() {
        super.init();
        doInConnection(connection -> {
            try (
                    PreparedStatement postStatement = connection.prepareStatement(INSERT_POST);
                    PreparedStatement postCommentStatement = connection.prepareStatement(INSERT_POST_COMMENT);
            ) {
                int postCount = getPostCount();
                int postCommentCount = getPostCommentCount();

                int index;

                for (int i = 0; i < postCount; i++) {
                    index = 0;
                    postStatement.setString(++index, String.format("Post no. %1$d", i));
                    postStatement.setInt(++index, 0);
                    postStatement.setLong(++index, i);
                    postStatement.executeUpdate();
                }

                for (int i = 0; i < postCount; i++) {
                    for (int j = 0; j < postCommentCount; j++) {
                        index = 0;
                        postCommentStatement.setLong(++index, i);
                        postCommentStatement.setString(++index, String.format("Post comment %1$d", j));
                        postCommentStatement.setInt(++index, (int) (Math.random() * 1000));
                        postCommentStatement.setLong(++index, (postCommentCount * i) + j);
                        postCommentStatement.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                fail(e.getMessage());
            }
        });
    }

    @Test
    public void testStatementCaching() {
        selectWhenCaching(true);
    }

    @Test
    public void testStatementWithoutCaching() {
        selectWhenCaching(false);
    }

    private void selectWhenCaching(boolean caching) {
        long startNanos = System.nanoTime();
        doInConnection(connection -> {
            OracleConnection oracleConnection = (OracleConnection) connection;
            assertTrue(oracleConnection.getImplicitCachingEnabled());
            assertEquals(5, oracleConnection.getStatementCacheSize());

            for (int i = 0; i < 100000; i++) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "select p.title, pc.review " +
                                "from post p left join postcomment pc on p.id = pc.post_id " +
                                "where EXISTS ( " +
                                "   select 1 from postcomment where version = ? and id > p.id " +
                                ")"
                )) {
                    statement.setPoolable(caching);
                    statement.setInt(1, i);
                    statement.execute();
                } catch (SQLException e) {
                    fail(e.getMessage());
                }
            }
        });
        LOGGER.info("{} when caching Statements is {} took {} millis",
                getClass().getSimpleName(),
                caching,
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
    }

    protected int getPostCount() {
        return 1000;
    }

    protected int getPostCommentCount() {
        return 5;
    }

    @Override
    protected boolean proxyDataSource() {
        return false;
    }
}
