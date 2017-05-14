package myeslib3.stack1.utils.jdbi;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultColumnMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class LocalDateTimeMapper implements ResultColumnMapper<LocalDateTime> {

	@Override
	public LocalDateTime mapColumn(ResultSet r, String columnLabel, StatementContext ctx) throws SQLException {
		final Timestamp timestamp = r.getTimestamp(columnLabel);
		if (timestamp == null) {
			return null;
		}
		return timestamp.toLocalDateTime();
	}

	@Override
	public LocalDateTime mapColumn(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
		final Timestamp timestamp = r.getTimestamp(columnNumber);
		if (timestamp == null) {
			return null;
		}
		return timestamp.toLocalDateTime();
	}
}