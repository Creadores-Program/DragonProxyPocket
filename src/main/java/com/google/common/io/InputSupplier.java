package com.google.common.io;

import java.io.IOException;

/** @deprecated */
@Deprecated
public interface InputSupplier<T> {
   T getInput() throws IOException;
}
