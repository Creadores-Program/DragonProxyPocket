package io.netty.handler.codec.http;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public class DefaultCookie implements Cookie {
   private final String name;
   private String value;
   private String rawValue;
   private String domain;
   private String path;
   private String comment;
   private String commentUrl;
   private boolean discard;
   private Set<Integer> ports = Collections.emptySet();
   private Set<Integer> unmodifiablePorts;
   private long maxAge;
   private int version;
   private boolean secure;
   private boolean httpOnly;

   public DefaultCookie(String name, String value) {
      this.unmodifiablePorts = this.ports;
      this.maxAge = Long.MIN_VALUE;
      if (name == null) {
         throw new NullPointerException("name");
      } else {
         name = name.trim();
         if (name.isEmpty()) {
            throw new IllegalArgumentException("empty name");
         } else {
            int i = 0;

            while(i < name.length()) {
               char c = name.charAt(i);
               if (c > 127) {
                  throw new IllegalArgumentException("name contains non-ascii character: " + name);
               }

               switch(c) {
               case '\t':
               case '\n':
               case '\u000b':
               case '\f':
               case '\r':
               case ' ':
               case ',':
               case ';':
               case '=':
                  throw new IllegalArgumentException("name contains one of the following prohibited characters: =,; \\t\\r\\n\\v\\f: " + name);
               default:
                  ++i;
               }
            }

            if (name.charAt(0) == '$') {
               throw new IllegalArgumentException("name starting with '$' not allowed: " + name);
            } else {
               this.name = name;
               this.setValue(value);
            }
         }
      }
   }

   public String name() {
      return this.name;
   }

   public String value() {
      return this.value;
   }

   public void setValue(String value) {
      if (value == null) {
         throw new NullPointerException("value");
      } else {
         this.value = value;
      }
   }

   public String rawValue() {
      return this.rawValue;
   }

   public void setRawValue(String rawValue) {
      if (this.value == null) {
         throw new NullPointerException("rawValue");
      } else {
         this.rawValue = rawValue;
      }
   }

   public String domain() {
      return this.domain;
   }

   public void setDomain(String domain) {
      this.domain = validateValue("domain", domain);
   }

   public String path() {
      return this.path;
   }

   public void setPath(String path) {
      this.path = validateValue("path", path);
   }

   public String comment() {
      return this.comment;
   }

   public void setComment(String comment) {
      this.comment = validateValue("comment", comment);
   }

   public String commentUrl() {
      return this.commentUrl;
   }

   public void setCommentUrl(String commentUrl) {
      this.commentUrl = validateValue("commentUrl", commentUrl);
   }

   public boolean isDiscard() {
      return this.discard;
   }

   public void setDiscard(boolean discard) {
      this.discard = discard;
   }

   public Set<Integer> ports() {
      if (this.unmodifiablePorts == null) {
         this.unmodifiablePorts = Collections.unmodifiableSet(this.ports);
      }

      return this.unmodifiablePorts;
   }

   public void setPorts(int... ports) {
      if (ports == null) {
         throw new NullPointerException("ports");
      } else {
         int[] portsCopy = (int[])ports.clone();
         if (portsCopy.length == 0) {
            this.unmodifiablePorts = this.ports = Collections.emptySet();
         } else {
            Set<Integer> newPorts = new TreeSet();
            int[] arr$ = portsCopy;
            int len$ = portsCopy.length;
            int i$ = 0;

            while(true) {
               if (i$ >= len$) {
                  this.ports = newPorts;
                  this.unmodifiablePorts = null;
                  break;
               }

               int p = arr$[i$];
               if (p <= 0 || p > 65535) {
                  throw new IllegalArgumentException("port out of range: " + p);
               }

               newPorts.add(p);
               ++i$;
            }
         }

      }
   }

   public void setPorts(Iterable<Integer> ports) {
      Set<Integer> newPorts = new TreeSet();
      Iterator i$ = ports.iterator();

      while(i$.hasNext()) {
         int p = (Integer)i$.next();
         if (p <= 0 || p > 65535) {
            throw new IllegalArgumentException("port out of range: " + p);
         }

         newPorts.add(p);
      }

      if (newPorts.isEmpty()) {
         this.unmodifiablePorts = this.ports = Collections.emptySet();
      } else {
         this.ports = newPorts;
         this.unmodifiablePorts = null;
      }

   }

   public long maxAge() {
      return this.maxAge;
   }

   public void setMaxAge(long maxAge) {
      this.maxAge = maxAge;
   }

   public int version() {
      return this.version;
   }

   public void setVersion(int version) {
      this.version = version;
   }

   public boolean isSecure() {
      return this.secure;
   }

   public void setSecure(boolean secure) {
      this.secure = secure;
   }

   public boolean isHttpOnly() {
      return this.httpOnly;
   }

   public void setHttpOnly(boolean httpOnly) {
      this.httpOnly = httpOnly;
   }

   public int hashCode() {
      return this.name().hashCode();
   }

   public boolean equals(Object o) {
      if (!(o instanceof Cookie)) {
         return false;
      } else {
         Cookie that = (Cookie)o;
         if (!this.name().equalsIgnoreCase(that.name())) {
            return false;
         } else {
            if (this.path() == null) {
               if (that.path() != null) {
                  return false;
               }
            } else {
               if (that.path() == null) {
                  return false;
               }

               if (!this.path().equals(that.path())) {
                  return false;
               }
            }

            if (this.domain() == null) {
               return that.domain() == null;
            } else {
               return that.domain() == null ? false : this.domain().equalsIgnoreCase(that.domain());
            }
         }
      }
   }

   public int compareTo(Cookie c) {
      int v = this.name().compareToIgnoreCase(c.name());
      if (v != 0) {
         return v;
      } else {
         if (this.path() == null) {
            if (c.path() != null) {
               return -1;
            }
         } else {
            if (c.path() == null) {
               return 1;
            }

            v = this.path().compareTo(c.path());
            if (v != 0) {
               return v;
            }
         }

         if (this.domain() == null) {
            return c.domain() != null ? -1 : 0;
         } else if (c.domain() == null) {
            return 1;
         } else {
            v = this.domain().compareToIgnoreCase(c.domain());
            return v;
         }
      }
   }

   public String toString() {
      StringBuilder buf = (new StringBuilder()).append(this.name()).append('=').append(this.value());
      if (this.domain() != null) {
         buf.append(", domain=").append(this.domain());
      }

      if (this.path() != null) {
         buf.append(", path=").append(this.path());
      }

      if (this.comment() != null) {
         buf.append(", comment=").append(this.comment());
      }

      if (this.maxAge() >= 0L) {
         buf.append(", maxAge=").append(this.maxAge()).append('s');
      }

      if (this.isSecure()) {
         buf.append(", secure");
      }

      if (this.isHttpOnly()) {
         buf.append(", HTTPOnly");
      }

      return buf.toString();
   }

   private static String validateValue(String name, String value) {
      if (value == null) {
         return null;
      } else {
         value = value.trim();
         if (value.isEmpty()) {
            return null;
         } else {
            int i = 0;

            while(i < value.length()) {
               char c = value.charAt(i);
               switch(c) {
               case '\n':
               case '\u000b':
               case '\f':
               case '\r':
               case ';':
                  throw new IllegalArgumentException(name + " contains one of the following prohibited characters: " + ";\\r\\n\\f\\v (" + value + ')');
               default:
                  ++i;
               }
            }

            return value;
         }
      }
   }
}
