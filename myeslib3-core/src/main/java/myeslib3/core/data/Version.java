package myeslib3.core.data;

import lombok.Value;

@Value
public class Version {

	private final long version;

	public Version(long version) {
		if (version <=0) throw new IllegalArgumentException("Version must be positive");
		this.version = version;
	}

	public static Version create(long version) {
		return new Version(version);
	}

	public Version nextVersion() {
		return new Version(version + 1);
	}

}
