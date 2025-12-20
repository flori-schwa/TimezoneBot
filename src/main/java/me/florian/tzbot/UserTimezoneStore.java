package me.florian.tzbot;

import java.sql.*;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.StampedLock;

public class UserTimezoneStore {

    private static final StampedLock LOCK = new StampedLock();

    private static final Map<Long, ZoneId> CACHE = new LinkedHashMap<>() {
        private static final int MAX_ENTRIES = 1_000_000;

        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, ZoneId> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    private UserTimezoneStore() {

    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:usertimezones.db");
    }

    public static void init() throws SQLException {
        final long writeLock = LOCK.writeLock();

        try (
                Connection connection = connect();
                Statement statement = connection.createStatement();
        ) {
            statement.executeUpdate("create table if not exists timezones(userid integer primary key, timezone text)");
        } finally {
            LOCK.unlockWrite(writeLock);
        }
    }

    public static ZoneId getUserZoneId(long userId) throws SQLException {
        long lock = LOCK.readLock();

        try {
            if (CACHE.containsKey(userId)) {
                return CACHE.get(userId);
            }

            long writeLock = LOCK.tryConvertToWriteLock(lock);

            if (writeLock != 0) {
                lock = writeLock;
            } else {
                LOCK.unlockRead(lock);
                lock = LOCK.writeLock();
            }

            try (
                    Connection connection = connect();
                    PreparedStatement statement = connection.prepareStatement("select timezone from timezones where userid = ?");
            ) {
                statement.setLong(1, userId);

                ResultSet resultSet = statement.executeQuery();

                if (resultSet.next()) {
                    final ZoneId zoneId = ZoneId.of(resultSet.getString("timezone"));
                    CACHE.put(userId, zoneId);
                    return zoneId;
                } else {
                    CACHE.put(userId, null);
                    return null;
                }
            }
        } finally {
            LOCK.unlock(lock);
        }
    }

    public static boolean saveUserTimezone(long userId, ZoneId zoneId) throws SQLException {
        final long writeLock = LOCK.writeLock();

        CACHE.put(userId, zoneId);

        try (Connection connection = connect()) {
            if (zoneId == null) {
                try (PreparedStatement statement = connection.prepareStatement("delete from timezones where userid = ?")) {
                    statement.setLong(1, userId);
                    return statement.executeUpdate() > 0;
                }
            } else {
                try (PreparedStatement statement = connection.prepareStatement("insert or replace into timezones values (?, ?)")) {
                    statement.setLong(1, userId);
                    statement.setString(2, zoneId.getId());

                    return statement.executeUpdate() > 0;
                }
            }
        } finally {
            LOCK.unlock(writeLock);
        }
    }

}
