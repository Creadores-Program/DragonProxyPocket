package javax.xml.namespace;

import java.io.Serializable;

public class QName implements Serializable {
   private String namespaceURI;
   private String localPart;
   private String prefix;

   public QName(String localPart) {
      this("", localPart);
   }

   public QName(String namespaceURI, String localPart) {
      this(namespaceURI, localPart, "");
   }

   public QName(String namespaceURI, String localPart, String prefix) {
      if (localPart == null) {
         throw new IllegalArgumentException("Local part not allowed to be null");
      } else {
         if (namespaceURI == null) {
            namespaceURI = "";
         }

         if (prefix == null) {
            prefix = "";
         }

         this.namespaceURI = namespaceURI;
         this.localPart = localPart;
         this.prefix = prefix;
      }
   }

   public String getNamespaceURI() {
      return this.namespaceURI;
   }

   public String getLocalPart() {
      return this.localPart;
   }

   public String getPrefix() {
      return this.prefix;
   }

   public String toString() {
      return this.namespaceURI.equals("") ? this.localPart : "{" + this.namespaceURI + "}" + this.localPart;
   }

   public static QName valueOf(String s) {
      if (s != null && !s.equals("")) {
         if (s.charAt(0) == '{') {
            int i = s.indexOf(125);
            if (i == -1) {
               throw new IllegalArgumentException("invalid QName literal");
            } else if (i == s.length() - 1) {
               throw new IllegalArgumentException("invalid QName literal");
            } else {
               return new QName(s.substring(1, i), s.substring(i + 1));
            }
         } else {
            return new QName(s);
         }
      } else {
         throw new IllegalArgumentException("invalid QName literal");
      }
   }

   public final int hashCode() {
      return this.namespaceURI.hashCode() ^ this.localPart.hashCode();
   }

   public final boolean equals(Object obj) {
      if (obj == null) {
         return false;
      } else if (!(obj instanceof QName)) {
         return false;
      } else {
         QName qname = (QName)obj;
         return this.localPart.equals(qname.localPart) && this.namespaceURI.equals(qname.namespaceURI);
      }
   }
}
