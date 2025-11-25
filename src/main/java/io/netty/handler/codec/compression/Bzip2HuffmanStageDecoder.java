package io.netty.handler.codec.compression;

final class Bzip2HuffmanStageDecoder {
   private final Bzip2BitReader reader;
   byte[] selectors;
   private final int[] minimumLengths;
   private final int[][] codeBases;
   private final int[][] codeLimits;
   private final int[][] codeSymbols;
   private int currentTable;
   private int groupIndex = -1;
   private int groupPosition = -1;
   final int totalTables;
   final int alphabetSize;
   final Bzip2MoveToFrontTable tableMTF = new Bzip2MoveToFrontTable();
   int currentSelector;
   final byte[][] tableCodeLengths;
   int currentGroup;
   int currentLength = -1;
   int currentAlpha;
   boolean modifyLength;

   Bzip2HuffmanStageDecoder(Bzip2BitReader reader, int totalTables, int alphabetSize) {
      this.reader = reader;
      this.totalTables = totalTables;
      this.alphabetSize = alphabetSize;
      this.minimumLengths = new int[totalTables];
      this.codeBases = new int[totalTables][25];
      this.codeLimits = new int[totalTables][24];
      this.codeSymbols = new int[totalTables][258];
      this.tableCodeLengths = new byte[totalTables][258];
   }

   void createHuffmanDecodingTables() {
      int alphabetSize = this.alphabetSize;

      for(int table = 0; table < this.tableCodeLengths.length; ++table) {
         int[] tableBases = this.codeBases[table];
         int[] tableLimits = this.codeLimits[table];
         int[] tableSymbols = this.codeSymbols[table];
         byte[] codeLengths = this.tableCodeLengths[table];
         int minimumLength = 23;
         int maximumLength = 0;

         int bitLength;
         for(bitLength = 0; bitLength < alphabetSize; ++bitLength) {
            byte currLength = codeLengths[bitLength];
            maximumLength = Math.max(currLength, maximumLength);
            minimumLength = Math.min(currLength, minimumLength);
         }

         this.minimumLengths[table] = minimumLength;

         for(bitLength = 0; bitLength < alphabetSize; ++bitLength) {
            ++tableBases[codeLengths[bitLength] + 1];
         }

         bitLength = 1;

         int code;
         for(code = tableBases[0]; bitLength < 25; ++bitLength) {
            code += tableBases[bitLength];
            tableBases[bitLength] = code;
         }

         bitLength = minimumLength;

         int symbol;
         for(code = 0; bitLength <= maximumLength; ++bitLength) {
            symbol = code;
            code += tableBases[bitLength + 1] - tableBases[bitLength];
            tableBases[bitLength] = symbol - tableBases[bitLength];
            tableLimits[bitLength] = code - 1;
            code <<= 1;
         }

         bitLength = minimumLength;

         for(code = 0; bitLength <= maximumLength; ++bitLength) {
            for(symbol = 0; symbol < alphabetSize; ++symbol) {
               if (codeLengths[symbol] == bitLength) {
                  tableSymbols[code++] = symbol;
               }
            }
         }
      }

      this.currentTable = this.selectors[0];
   }

   int nextSymbol() {
      if (++this.groupPosition % 50 == 0) {
         ++this.groupIndex;
         if (this.groupIndex == this.selectors.length) {
            throw new DecompressionException("error decoding block");
         }

         this.currentTable = this.selectors[this.groupIndex] & 255;
      }

      Bzip2BitReader reader = this.reader;
      int currentTable = this.currentTable;
      int[] tableLimits = this.codeLimits[currentTable];
      int[] tableBases = this.codeBases[currentTable];
      int[] tableSymbols = this.codeSymbols[currentTable];
      int codeLength = this.minimumLengths[currentTable];

      for(int codeBits = reader.readBits(codeLength); codeLength <= 23; ++codeLength) {
         if (codeBits <= tableLimits[codeLength]) {
            return tableSymbols[codeBits - tableBases[codeLength]];
         }

         codeBits = codeBits << 1 | reader.readBits(1);
      }

      throw new DecompressionException("a valid code was not recognised");
   }
}
