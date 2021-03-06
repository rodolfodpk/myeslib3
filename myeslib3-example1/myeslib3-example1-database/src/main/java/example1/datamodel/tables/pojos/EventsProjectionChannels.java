/*
 * This file is generated by jOOQ.
*/
package example1.datamodel.tables.pojos;


import javax.annotation.Generated;
import java.io.Serializable;


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
public class EventsProjectionChannels implements Serializable {

    private static final long serialVersionUID = 1965066701;

    private String channelName;
    private Long   uowLastSeq;

    public EventsProjectionChannels() {}

    public EventsProjectionChannels(EventsProjectionChannels value) {
        this.channelName = value.channelName;
        this.uowLastSeq = value.uowLastSeq;
    }

    public EventsProjectionChannels(
        String channelName,
        Long   uowLastSeq
    ) {
        this.channelName = channelName;
        this.uowLastSeq = uowLastSeq;
    }

    public String getChannelName() {
        return this.channelName;
    }

    public EventsProjectionChannels setChannelName(String channelName) {
        this.channelName = channelName;
        return this;
    }

    public Long getUowLastSeq() {
        return this.uowLastSeq;
    }

    public EventsProjectionChannels setUowLastSeq(Long uowLastSeq) {
        this.uowLastSeq = uowLastSeq;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("EventsProjectionChannels (");

        sb.append(channelName);
        sb.append(", ").append(uowLastSeq);

        sb.append(")");
        return sb.toString();
    }
}
