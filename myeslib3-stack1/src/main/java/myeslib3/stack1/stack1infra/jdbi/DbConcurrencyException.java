package myeslib3.stack1.stack1infra.jdbi;

public class DbConcurrencyException extends RuntimeException {

	static final long serialVersionUID = -7034897440745766939L;

	public DbConcurrencyException(String s) {
		super(s);
	}
}
