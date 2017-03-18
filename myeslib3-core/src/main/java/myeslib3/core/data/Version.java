package myeslib3.core.data;


public class Version {

	private final Long version;

	public Version(Long version) {
		this.version = version;
	}

	public static Version create(Long version) {
		return new Version(version);
	}

	public static Version create(Integer version) {
		return new Version(version.longValue());
	}

	public Version nextVersion() {
		return new Version(version + 1);
	}

	public Long getVersion() {
		return version;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Version version1 = (Version) o;

		return version.equals(version1.version);
	}

	@Override
	public int hashCode() {
		return version.hashCode();
	}

	@Override
	public String toString() {
		return "Version{" +
						"version=" + version +
						'}';
	}
}
