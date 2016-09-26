package de.c.seiler.opticsannotation.processor.util;

public class Strings
{
  public static String capitalize(String s)
  {
    if (s == null || s.isEmpty())
      return s;
    char[] ca = s.toCharArray();
    boolean next = true;

    for (int i = 0; i < ca.length; i++)
    {
      char c = ca[i];
      if (Character.isWhitespace(c))
        next = true;
      else if (next)
      {
        ca[i] = Character.toTitleCase(c);
        next = false;
      }
    }
    String result = new String(ca);
    return result;
  }

  public static boolean isPrimitive(String className)
  {
    if ("int".equals(className) || "long".equals(className) || "double".equals(className))
      return true;
    return false;
  }
  
  public static boolean isPrimitiveBoolean(String className)
  {
    return "boolean".equals(className);
  }
}
