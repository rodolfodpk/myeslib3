package myeslib3.stack1.infra.jdbi;

public class DbConcurrencyException extends RuntimeException {
	public DbConcurrencyException(String s) {
		super(s);
	}
}
