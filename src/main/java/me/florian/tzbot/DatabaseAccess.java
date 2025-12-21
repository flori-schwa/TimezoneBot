package me.florian.tzbot;

import me.florian.tzbot.util.Cache;

import java.sql.*;
import java.time.ZoneId;
import java.util.concurrent.locks.StampedLock;

public final class DatabaseAccess {

    private static final StampedLock DB_LOCK = new StampedLock();

    private static final Cache<Long, ZoneId> CACHE = new Cache<>(1_000_000);

    private DatabaseAccess() {

    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:usertimezones.db");
    }

    public static void init() throws SQLException {
        final long writeLock = DB_LOCK.writeLock();

        try (
                Connection connection = connect();
                Statement statement = connection.createStatement();
        ) {
            statement.executeUpdate("create table if not exists timezones(userid integer primary key, timezone text)");
        } finally {
            DB_LOCK.unlockWrite(writeLock);
        }
    }

    public static ZoneId getUserZoneId(long userId) throws SQLException {
        return CACHE.computeIfAbsent(userId, DatabaseAccess::readUserZoneIdFromDb);
    }

    private static ZoneId readUserZoneIdFromDb(long userId) throws SQLException {
        long lock = DB_LOCK.readLock();

        try (
                Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement("select timezone from timezones where userid = ?");
        ) {
            statement.setLong(1, userId);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return ZoneId.of(resultSet.getString("timezone"));
            } else {
                return null;
            }
        } finally {
            DB_LOCK.unlockRead(lock);
        }
    }

    public static boolean saveUserTimezone(long userId, ZoneId zoneId) throws SQLException {
        final long writeLock = DB_LOCK.writeLock();

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
            DB_LOCK.unlockWrite(writeLock);
        }
    }

}
