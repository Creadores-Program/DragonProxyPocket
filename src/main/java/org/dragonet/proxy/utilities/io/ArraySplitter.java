package org.dragonet.proxy.utilities.io;

import java.util.Arrays;

public final class ArraySplitter {
   public static byte[][] splitArray(byte[] array, int singleSlice) {
      byte[][] ret;
      if (array.length <= singleSlice) {
         ret = new byte[][]{array};
         return ret;
      } else {
         ret = new byte[array.length / singleSlice + (array.length % singleSlice == 0 ? 0 : 1)][];
         int pos = 0;
         int slice = 0;

         while(slice < ret.length) {
            if (pos + singleSlice < array.length) {
               ret[slice] = Arrays.copyOfRange(array, pos, pos + singleSlice);
               pos += singleSlice;
               ++slice;
            } else {
               ret[slice] = Arrays.copyOfRange(array, pos, array.length);
               pos += array.length - pos;
               ++slice;
            }
         }

         return ret;
      }
   }
}
