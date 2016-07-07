package me.desht.checkers.results;

import java.sql.Connection;
import java.sql.SQLException;

public interface DatabaseSavable {
	void saveToDatabase(Connection conn) throws SQLException;
}
