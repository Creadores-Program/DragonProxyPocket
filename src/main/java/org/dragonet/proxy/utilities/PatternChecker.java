package org.dragonet.proxy.utilities;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PatternChecker {
   public static final String REGEX_EMAIL = "^([a-z0-9A-Z]+[-|_|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$";
   public static final Pattern PATTERN_EMAIL = Pattern.compile("^([a-z0-9A-Z]+[-|_|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$");

   public static boolean matchEmail(String email) {
      Matcher matcher = PATTERN_EMAIL.matcher(email);
      return matcher.matches();
   }
}
