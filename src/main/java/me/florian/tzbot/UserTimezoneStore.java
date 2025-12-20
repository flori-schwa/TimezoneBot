package me.florian.tzbot;

import java.sql.*;
import java.time.ZoneId;
import java.util.Optional;

public class UserTimezoneStore {

    private UserTimezoneStore() {

    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:usertimezones.db");
    }

    public static void init() throws SQLException {
        try (
                Connection connection = connect();
                Statement statement = connection.createStatement();
        ) {
            statement.executeUpdate("create table if not exists timezones(userid integer primary key, timezone text)");
        }
    }

    public static Optional<ZoneId> getUserZoneId(long userId) throws SQLException {
        try (
            Connection connection = connect();
            PreparedStatement statement = connection.prepareStatement("select timezone from timezones where userid = ?");
        ) {
            statement.setLong(1, userId);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return Optional.of(ZoneId.of(resultSet.getString("timezone")));
            } else {
                return Optional.empty();
            }
        }
    }

    public static boolean saveUserTimezone(long userId, ZoneId zoneId) throws SQLException {
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
        }
    }

}
