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
public class Idempotency implements Serializable {

    private static final long serialVersionUID = -368195678;

    private String    slotName;
    private String    slotId;
    private Timestamp insertedOn;

    public Idempotency() {}

    public Idempotency(Idempotency value) {
        this.slotName = value.slotName;
        this.slotId = value.slotId;
        this.insertedOn = value.insertedOn;
    }

    public Idempotency(
        String    slotName,
        String    slotId,
        Timestamp insertedOn
    ) {
        this.slotName = slotName;
        this.slotId = slotId;
        this.insertedOn = insertedOn;
    }

    public String getSlotName() {
        return this.slotName;
    }

    public Idempotency setSlotName(String slotName) {
        this.slotName = slotName;
        return this;
    }

    public String getSlotId() {
        return this.slotId;
    }

    public Idempotency setSlotId(String slotId) {
        this.slotId = slotId;
        return this;
    }

    public Timestamp getInsertedOn() {
        return this.insertedOn;
    }

    public Idempotency setInsertedOn(Timestamp insertedOn) {
        this.insertedOn = insertedOn;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Idempotency (");

        sb.append(slotName);
        sb.append(", ").append(slotId);
        sb.append(", ").append(insertedOn);

        sb.append(")");
        return sb.toString();
    }
}
