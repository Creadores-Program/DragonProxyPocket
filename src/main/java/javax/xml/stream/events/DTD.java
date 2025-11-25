package javax.xml.stream.events;

import java.util.List;

public interface DTD extends XMLEvent {
   String getDocumentTypeDeclaration();

   Object getProcessedDTD();

   List getNotations();

   List getEntities();
}
