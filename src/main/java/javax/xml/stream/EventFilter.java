package javax.xml.stream;

import javax.xml.stream.events.XMLEvent;

public interface EventFilter {
   boolean accept(XMLEvent var1);
}
