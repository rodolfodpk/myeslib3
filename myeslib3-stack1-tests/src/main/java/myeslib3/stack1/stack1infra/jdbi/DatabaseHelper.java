package myeslib3.stack1.stack1infra.jdbi;

import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.io.Resources;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;

import static java.util.Objects.requireNonNull;

public class DatabaseHelper {

	private final DBI dbi;
	private final String ddlScriptFile;

	static final Logger logger = LoggerFactory.getLogger(DatabaseHelper.class);

	public DatabaseHelper(DBI dbi, String ddlScriptFile) {
		requireNonNull(dbi);
		this.dbi = dbi;
		requireNonNull(ddlScriptFile);
		this.ddlScriptFile = ddlScriptFile;
	}

	public void initDb() {
		try {
			Handle h = dbi.open();
			for (String statement : statements()) {
				logger.debug("executing {}", statement);
				h.execute(statement);
			}
			h.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Iterable<String> statements() throws IOException {
		URL url = Resources.getResource(ddlScriptFile);
		String content = Resources.toString(url, Charsets.UTF_8);
		return Splitter.on(CharMatcher.is(';'))
						.trimResults()
						.omitEmptyStrings()
						.split(content);
	}
}

