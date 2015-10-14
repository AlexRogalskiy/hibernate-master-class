package com.vladmihalcea.book.high_performance_java_persistence.jdbc.transaction;

import com.microsoft.sqlserver.jdbc.SQLServerConnection;
import org.junit.runners.Parameterized;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * SQLServerPhenomenaTest - Test to validate SQL Server phenomena
 *
 * @author Vlad Mihalcea
 */
public class SQLServerPhenomenaTest extends AbstractPhenomenaTest {

    public SQLServerPhenomenaTest(String isolationLevelName, int isolationLevel) {
        super(isolationLevelName, isolationLevel);
    }

    @Override
    protected DataSourceProvider getDataSourceProvider() {
        return new SQLServerDataSourceProvider();
    }

    @Parameterized.Parameters
    public static Collection<Object[]> isolationLevels() {
        List<Object[]> levels = new ArrayList<>();
        levels.add(new Object[]{"Read Uncommitted", Connection.TRANSACTION_READ_UNCOMMITTED});
        levels.add(new Object[]{"Read Committed", Connection.TRANSACTION_READ_COMMITTED});
        levels.add(new Object[]{"Repeatable Read", Connection.TRANSACTION_REPEATABLE_READ});
        levels.add(new Object[]{"Serializable", Connection.TRANSACTION_SERIALIZABLE});
        levels.add(new Object[]{"Read Committed Snapshot Isolation", SQLServerConnection.TRANSACTION_READ_COMMITTED});
        levels.add(new Object[]{"Snapshot Isolation", SQLServerConnection.TRANSACTION_SNAPSHOT});
        return levels;
    }

    protected String dirtyReadSql() {
        return "SELECT title FROM post WITH(NOWAIT) WHERE id = 1";
    }

    @Override
    protected void prepareConnection(Connection connection) throws SQLException {
        try(Statement statement = connection.createStatement()) {
            String snapshot = getIsolationLevelName().contains("Snapshot") ? "ON" : "OFF";
            statement.executeUpdate("ALTER DATABASE hibernate_master_class SET READ_COMMITTED_SNAPSHOT " + snapshot);
            statement.executeUpdate("ALTER DATABASE hibernate_master_class SET ALLOW_SNAPSHOT_ISOLATION " + snapshot);
        }
        super.prepareConnection(connection);
    }
}
