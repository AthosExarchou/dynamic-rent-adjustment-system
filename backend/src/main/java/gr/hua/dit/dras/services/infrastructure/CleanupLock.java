package gr.hua.dit.dras.services.infrastructure;

/* imports */
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Serializes cleanup runs.
 */
@Component
public class CleanupLock {

    private static final Logger log = LoggerFactory.getLogger(CleanupLock.class);
    private static final long LOCK_ID = 20261121L;

    private final DataSource dataSource;
    private final ReentrantLock localLock = new ReentrantLock();

    private Connection heldConnection;

    public CleanupLock(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean tryLock() {
        if (!localLock.tryLock()) {
            log.info("Cleanup already running in this JVM");
            return false;
        }

        Connection connection = null;
        boolean locked = false;
        try {
            connection = dataSource.getConnection();
            if (isPostgres(connection)) {
                try (PreparedStatement statement = connection.prepareStatement("SELECT pg_try_advisory_lock(?)")) {
                    statement.setLong(1, LOCK_ID);
                    try (ResultSet result = statement.executeQuery()) {
                        locked = result.next() && result.getBoolean(1);
                    }
                }
                if (!locked) {
                    log.info("Cleanup already running on another backend instance");
                }
            } else {
                locked = true;
            }
        } catch (SQLException e) {
            log.warn("Could not acquire database advisory lock, skipping cleanup run", e);
            locked = false;
        }

        if (!locked) {
            closeQuietly(connection);
            localLock.unlock();
            return false;
        }

        heldConnection = connection;
        return true;
    }

    public void unlock() {
        Connection connection = heldConnection;
        heldConnection = null;
        try {
            if (connection != null && isPostgres(connection)) {
                try (PreparedStatement statement = connection.prepareStatement("SELECT pg_advisory_unlock(?)")) {
                    statement.setLong(1, LOCK_ID);
                    statement.execute();
                }
            }
        } catch (SQLException e) {
            log.warn("Could not release database advisory lock", e);
        } finally {
            closeQuietly(connection);
            localLock.unlock();
        }
    }

    private boolean isPostgres(Connection connection) throws SQLException {
        String product = connection.getMetaData().getDatabaseProductName();
        return product != null && product.toLowerCase(Locale.ROOT).contains("postgresql");
    }

    private void closeQuietly(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException e) {
            log.warn("Could not close cleanup lock connection", e);
        }
    }
}
