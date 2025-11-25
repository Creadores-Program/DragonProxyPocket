package org.dragonet.proxy.utilities.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public class DataIOPair {
   private DataInputStream input;
   private DataOutputStream output;

   public DataIOPair(DataInputStream input, DataOutputStream output) {
      this.input = input;
      this.output = output;
   }

   public DataInputStream getInput() {
      return this.input;
   }

   public DataOutputStream getOutput() {
      return this.output;
   }
}
