package org.codehaus.jettison.util;

import java.util.ArrayList;
import java.util.EmptyStackException;

public class FastStack extends ArrayList {
   public void push(Object o) {
      this.add(o);
   }

   public Object pop() {
      if (this.empty()) {
         throw new EmptyStackException();
      } else {
         return this.remove(this.size() - 1);
      }
   }

   public boolean empty() {
      return this.size() == 0;
   }

   public Object peek() {
      if (this.empty()) {
         throw new EmptyStackException();
      } else {
         return this.get(this.size() - 1);
      }
   }
}
