package com.google.common.io;

import java.io.IOException;

/** @deprecated */
@Deprecated
public interface OutputSupplier<T> {
   T getOutput() throws IOException;
}
