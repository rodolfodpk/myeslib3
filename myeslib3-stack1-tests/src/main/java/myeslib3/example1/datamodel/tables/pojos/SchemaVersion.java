/*
 * This file is generated by jOOQ.
*/
package myeslib3.example1.datamodel.tables.pojos;


import javax.annotation.Generated;
import java.io.Serializable;
import java.sql.Timestamp;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.9.2"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class SchemaVersion implements Serializable {

    private static final long serialVersionUID = 1616409207;

    private Integer   installedRank;
    private String    version;
    private String    description;
    private String    type;
    private String    script;
    private Integer   checksum;
    private String    installedBy;
    private Timestamp installedOn;
    private Integer   executionTime;
    private Byte      success;

    public SchemaVersion() {}

    public SchemaVersion(SchemaVersion value) {
        this.installedRank = value.installedRank;
        this.version = value.version;
        this.description = value.description;
        this.type = value.type;
        this.script = value.script;
        this.checksum = value.checksum;
        this.installedBy = value.installedBy;
        this.installedOn = value.installedOn;
        this.executionTime = value.executionTime;
        this.success = value.success;
    }

    public SchemaVersion(
        Integer   installedRank,
        String    version,
        String    description,
        String    type,
        String    script,
        Integer   checksum,
        String    installedBy,
        Timestamp installedOn,
        Integer   executionTime,
        Byte      success
    ) {
        this.installedRank = installedRank;
        this.version = version;
        this.description = description;
        this.type = type;
        this.script = script;
        this.checksum = checksum;
        this.installedBy = installedBy;
        this.installedOn = installedOn;
        this.executionTime = executionTime;
        this.success = success;
    }

    public Integer getInstalledRank() {
        return this.installedRank;
    }

    public SchemaVersion setInstalledRank(Integer installedRank) {
        this.installedRank = installedRank;
        return this;
    }

    public String getVersion() {
        return this.version;
    }

    public SchemaVersion setVersion(String version) {
        this.version = version;
        return this;
    }

    public String getDescription() {
        return this.description;
    }

    public SchemaVersion setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getType() {
        return this.type;
    }

    public SchemaVersion setType(String type) {
        this.type = type;
        return this;
    }

    public String getScript() {
        return this.script;
    }

    public SchemaVersion setScript(String script) {
        this.script = script;
        return this;
    }

    public Integer getChecksum() {
        return this.checksum;
    }

    public SchemaVersion setChecksum(Integer checksum) {
        this.checksum = checksum;
        return this;
    }

    public String getInstalledBy() {
        return this.installedBy;
    }

    public SchemaVersion setInstalledBy(String installedBy) {
        this.installedBy = installedBy;
        return this;
    }

    public Timestamp getInstalledOn() {
        return this.installedOn;
    }

    public SchemaVersion setInstalledOn(Timestamp installedOn) {
        this.installedOn = installedOn;
        return this;
    }

    public Integer getExecutionTime() {
        return this.executionTime;
    }

    public SchemaVersion setExecutionTime(Integer executionTime) {
        this.executionTime = executionTime;
        return this;
    }

    public Byte getSuccess() {
        return this.success;
    }

    public SchemaVersion setSuccess(Byte success) {
        this.success = success;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SchemaVersion (");

        sb.append(installedRank);
        sb.append(", ").append(version);
        sb.append(", ").append(description);
        sb.append(", ").append(type);
        sb.append(", ").append(script);
        sb.append(", ").append(checksum);
        sb.append(", ").append(installedBy);
        sb.append(", ").append(installedOn);
        sb.append(", ").append(executionTime);
        sb.append(", ").append(success);

        sb.append(")");
        return sb.toString();
    }
}
