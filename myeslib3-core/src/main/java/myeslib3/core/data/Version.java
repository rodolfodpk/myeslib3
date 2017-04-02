package myeslib3.core.data;

import lombok.Value;

@Value
public class Version {

	private final Long version;

	public static Version create(Long version) {
		return new Version(version);
	}

	public static Version create(Integer version) {
		return new Version(version.longValue());
	}

	public Version nextVersion() {
		return new Version(version + 1);
	}

}
