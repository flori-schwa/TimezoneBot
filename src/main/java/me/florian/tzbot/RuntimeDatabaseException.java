package me.florian.tzbot;

import java.sql.SQLException;

public class RuntimeDatabaseException extends RuntimeException {

    public RuntimeDatabaseException(SQLException cause) {
        super(cause);
    }

}
