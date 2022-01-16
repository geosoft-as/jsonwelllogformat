package no.geosoft.jwlf;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * A collection of utilities for the Log I/O library.
 *
 * @author <a href="mailto:jacob.dreyer@geosoft.no">Jacob Dreyer</a>
 */
public final class Util
{
  /** Pre-created strings of spaces of a given length. */
  private final static String[] SPACES = new String[100];

  /**
   * Initialize static members of this class.
   */
  static {
    StringBuilder s = new StringBuilder("");
    for (int i = 0; i < SPACES.length; i++) {
      SPACES[i] = s.toString();
      s.append(' ');
    }
  }

  /**
   * Private constructor to prevent client instantiation.
   */
  private Util()
  {
    assert false : "This constructor should never be called";
  }

  /**
   * Split the specified text in tokens based on the given delimiter.
   * <p>
   * Some special cases:
   * <ul>
   *   <li>Tokens can be in quotes, in case they can contain the delimiter</li>
   *   <li>Quotes is pair of " or ' characters</li>
   *   <li>Token in quotes can contain quote characters of the other type</li>
   *   <li>Tokens are always trimmed unless they are in quotes</li>
   *   <li>If delimiter is space, no empty tokens are returned (unless quoted)</li>
   * </ul>
   * Examples:
   * <pre>
   *   "1 2 3 4"            split on ' ' gives "1","2","3","4"
   *   "   1  2  3 4  "     split on ' ' gives "1","2","3","4"
   *   "   '1 2' 3 '4 ' ''" split on ' ' gives "1 2","3","4 ",""
   *   "1 2 '3"' 4"         split on ' ' gives "1","2","3"","4"
   *   " "                  split on ' ' gives nothing
   *   ","                  split on ',' gives "",""
   *   "1,2,3,4"            split on ',' gives "1","2","3","4"
   *   "  1,  2,  3,  4 "   split on ',' gives "1","2","3","4"
   *   ",,1,2,,3,4,5,,"     split on ',' gives "","","1","2","","3","4","5","",""
   * </pre>
   * Issue: How to handle this case:
   * <pre>
   *   "1 2 '3''4' 5"  split on ' ' gives "1","2","34","5"
   * </pre>
   * Not sure what the optimal solution should be.
   * <p>
   * <b>Performace:</b>
   * <p>
   * For comma (or similar) delimited strings without quotes the String.split()
   * method will behave slightly better (40%). For space delimited strings where
   * "\\s+" is used to suppress additional whitespace, this method is about 3x faster.
   * It is possible to use String.split() with regex to handle quotes, but it is
   * slower, and it will leave the quotes in there so they must be removed afterwards.
   *
   * @param text       Text to split. Non-null.
   * @param delimiter  Delimiter character such as comma or space etc.
   * @return           The tokens of the text. Never null.
   */
  public static List<String> split(String text, char delimiter)
  {
    if (text == null)
      throw new IllegalArgumentException("text cannot be null");

    final boolean isSpaceDelimited = delimiter == ' ';

    // Prepare the return array
    List<String> tokens = new ArrayList<>();

    // If not equal '\0' this indicates that we are inside a quoted string
    // and the type of quotes
    char inString = '\0';

    // We don't trim quoted text so keep track if the current is quoted
    boolean isTokenQuoted = false;

    String token;

    // Pointers indicating substring that make up the next token
    int p0 = 0;
    int p1 = 0;

    char prevChar = '\0';
    char currentChar = '\0';

    for (int i = 0; i < text.length(); i++) {
      prevChar = currentChar;
      currentChar = text.charAt(i);

      boolean isQuote = currentChar == '\"' || currentChar == '\'';

      // Start quote
      if (isQuote && inString == '\0') {
        inString = currentChar;
        isTokenQuoted = true;
        p0 = i + 1;
        p1 = p0;
        continue;
      }

      // End quote
      if (isQuote && inString == currentChar) {
        inString = '\0';
        continue;
      }

      // Delimiter that is not inside quotes
      if (currentChar == delimiter && inString == '\0') {
        token = isTokenQuoted ? text.substring(p0, p1) : text.substring(p0, p1).trim();
        if (!token.isEmpty() || !isSpaceDelimited || isTokenQuoted)
          tokens.add(token);

        p0 = isTokenQuoted ? p1 + 2 : p1 + 1;
        p1 = p0;

        isTokenQuoted = false;
        continue;
      }

      p1++;
    }

    // We are done. Capture that last token (if any)
    token = isTokenQuoted ? text.substring(p0, p1) : text.substring(p0, p1).trim();
    if (!token.isEmpty() || !isSpaceDelimited || isTokenQuoted)
      tokens.add(token);

    return tokens;
  }

  /**
   * Return the number of digits in the specified number.
   *
   * @param v  Number to count digits in.
   * @return   Number of digits in v. [1,&gt;.
   */
  private static int countDigits(long v)
  {
    // Twice as fast as (Math.abs(v) + "").length()
    return v == 0 ? 1 : (int) Math.log10(Math.abs(v)) + 1;
  }

  /**
   * Return number of significant decimals in the specified
   * floating point value.
   * <blockquote>
   *   <pre>
   *   0.991 = 3
   *   0.9901 = 4
   *   0.99001 = 5
   *   0.990001 = 6
   *   0.9900001 = 2
   *   </pre>
   * </blockquote>
   * However, if the whole part is large this will influence the result
   * as there is a maximum number of significant digits representable:
   * <blockquote>
   *   <pre>
   *   12345678.991 = 3
   *   12345678.9901 = 4
   *   12345678.99001 = 4   // ideally 2 but we don't capture this
   *   12345678.990001 = 4  // ideally 2 but we don't capture this
   *   </pre>
   * </blockquote>
   *
   * @param d  Number to check.
   * @return   Number of significant decimals. [0,&gt;.
   */
  private static int countDecimals(double d)
  {
    if (!Double.isFinite(d))
      return 0;

    // We strip away sign and the whole part as we care about
    // the decimals only. Left is something like 0.12....
    d = Math.abs(d);
    long wholePart = Math.round(d);
    int nSignificant = countDigits(wholePart);
    double fractionPart = Math.abs(d - wholePart);

    // We start with the fraction, say 0.12345678 and loop
    // over it 10x at the time, so for this example we will get:
    //   0.12345678
    //   1.2345678
    //   12.345678
    //   123.45678
    //   :
    int nDecimals = 0;
    int order = 1;
    while (true) {

      // This is the full floating point number and the integer part
      // (rounded) i.e. 12.789 and 13 etc.
      double floating = fractionPart * order;
      long whole = Math.round(floating);

      // We find the difference between the two, like 0.211 and find
      // what is the fraction of this with the whole.
      double difference = Math.abs(whole - floating);
      double fraction = whole != 0.0 ? difference / whole : difference;

      // If this fraction is very low then the fractional part of the
      // number no longer contributes very much to the whole and we
      // conclude that the rest is not significant decimals of this number.
      if (fraction < 0.0001)
        break;

      // If we reach maximum number of signigicant digit the computer
      // can normally represent we stop:
      // 1234567890.12345678 is 1234567890.12 and nDecimals are 2.
      if (nSignificant >= 12)
        break;

      order *= 10;
      nDecimals++;
      nSignificant++;
    }

    return nDecimals;
  }

  /**
   * Return the number of significant digits needed to properly represent
   * values of regular data of the specified step and (maximum) magnitude.
   * <p>
   * The intent is to find the correct number of significant digits in order
   * to properly preserve the regularity of such numbers.
   *
   * @param magnitude  Magnitude of the numbers in question. Typically
   *                   the max value of the range.
   * @param step       The regular step value.
   * @return           The requested number of significant digits. [0,&gt;.
   */
  public static int getNSignificantDigits(double magnitude, double step)
  {
    // Count the number of digits in the (absolute of the) magnitude
    int nDigits = (int) Math.round(Math.abs(Math.log10(Math.abs(magnitude))) + 0.5);

    // Count the numbers of significant decimals in the regular step
    // We will report at least 1 to keep significance within one order of magnitude
    // of the step.
    int nDecimals = Math.max(1, countDecimals(step));
    int nSignificantDigits = nDigits + nDecimals;

    // Limit ourself to the capabilities of the platform
    if (nSignificantDigits > 10)
      nSignificantDigits = 10;

    return nSignificantDigits;
  }

  /**
   * Format the specified floating point number to a string
   * with the most sensible number of decimals.
   *
   * @param d  Double number to format.
   * @return   Associated string representation.
   */
  public static String toString(double d)
  {
    int nDecimals = countDecimals(d);
    String formatString = "%." + nDecimals + "f";
    return String.format(Locale.US, formatString, d);
  }

  /**
   * Return a fixed length string version of the specified text,
   * either right clipped or right padded with spaces.
   *
   * @param text     Text to return. May be null.
   * @param length   Length of returned text. [0,&gt;.
   * @return         Requested sring. Never null.
   */
  public static String toString(String text, int length)
  {
    if (length < 0)
      throw new IllegalArgumentException("Invalid length: " + length);

    // Special case as String.format doesn't accespt 0 argument
    if (length == 0)
      return "";

    String s = text != null ? text : "";
    String formatString = "%-" + length + "." + length + "s";

    return String.format(formatString, s);
  }

  /**
   * Return the UTF-8 string representation of the specified object as a
   * specific length byte array.
   *
   * @param object   Object to get string representation of. May be null.
   * @param nBytes   Number of bytes in returned value. [0,&gt;.
   * @return         The requested string representation. Never null.
   */
  public static byte[] toUtf8(Object object, int nBytes)
  {
    if (nBytes < 0)
      throw new IllegalArgumentException("Invalid nBytes: " + nBytes);

    byte[] bytes = new byte[nBytes];

    String text = object != null ? object.toString() : "";
    byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);

    //
    // Case 1: The string fits fully in the allocated array
    //
    if (textBytes.length <= nBytes) {

      // Copy the string content
      System.arraycopy(textBytes, 0, bytes, 0, textBytes.length);

      // Pad with spaces
      for (int i = textBytes.length; i < nBytes; i++)
        bytes[i] = ' ';
    }

    //
    // Case 2: We must clip. Since characters can vary in size we
    //         must be careful: We cannot cut a character in half
    //         (that would leave a different character, or a non-printable)
    //         we must instead cut before and pad with spaces.
    //
    else {
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      OutputStreamWriter writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8);
      int n = 0;
      for (int i = 0; i < text.length(); i++) {
        try {
          // Write one character (1, 2 or 3 byte) to the output
          writer.write(text, i, 1);
          writer.flush();

          // Check the byte size
          int size = stream.size();

          // If we got past the allocated length, we stop here and
          // stick with the n we got from previous character
          if (size > nBytes)
            break;

          n = size;
        }
        catch (IOException exception) {
          // This will not happen, but if it does we just pad with spaces
          break;
        }
      }

      // Copy the n bytes that we could fit into the output
      System.arraycopy(stream.toByteArray(), 0, bytes, 0, n);

      // Pad with spaces
      for (int i = n; i < nBytes; i++)
        bytes[i] = ' ';
    }

    return bytes;
  }

  /**
   * Return a string containing the specified number of
   * space characters.
   *
   * @param n  Length of string to create.
   * @return   Requested string. If n is less than or equal to
   *           0 an empty string is returned. Never null.
   */
  public static String getSpaces(int n)
  {
    // TODO: Not sure why we allow n < 0 here
    if (n <= 0)
      return "";

    if (n < SPACES.length)
      return SPACES[n];

    // This slightly faster than using String.format().
    // Combined with the caching it is a lot faster for typical strings.
    StringBuilder s = new StringBuilder("");
    for (int i = 0; i < n; i++)
      s.append(' ');

    return s.toString();
  }

  /**
   * Return the positions of the c character inside
   * the specified string.
   *
   * @param s  String to check. Non-null.
   * @param c  Character to search for.
   * @return   Index positions of c within s.
   */
  public static int[] pos(String s, char c)
  {
    assert s != null : "s cannot be null";

    List<Integer> pos = new ArrayList<>();

    int i = 0;
    while (i < s.length()) {
      int p = s.indexOf(c, i);

      if (p == -1)
        break;

      pos.add(p);
      i = p + 1;
    }

    int[] a = new int[pos.size()];
    i = 0;
    for (Integer value : pos)
      a[i++] = value;

    return a;
  }

  /**
   * Check if a given object represents a numeric value.
   *
   * @param value  Value to check. May be null, in case false will be returned.
   * @return  True is value is a numeric value, false otherwise.
   */
  public static boolean isNumeric(Object value)
  {
    if (value == null)
      return false;

    if (value instanceof Number)
      return true;

    try {
      Double.parseDouble(value.toString());
      return true;
    }
    catch (NumberFormatException exception) {
      return false;
    }
  }

  /**
   * Check if the specified value type is numeric.
   *
   * @param valueType  Value type to check. Non-null.
   * @return           True if the value type is numeric, false otherwise.
   * @throws IllegalArgumentException  If valueType is null.
   */
  public static boolean isNumeric(Class<?> valueType)
  {
    if (valueType == null)
      throw new IllegalArgumentException("valueType cannot be null");

    return Number.class.isAssignableFrom(valueType) ||
           valueType == double.class ||
           valueType == float.class ||
           valueType == long.class ||
           valueType == int.class ||
           valueType == short.class ||
           valueType == byte.class;
  }

  /**
   * Convenience method to return the specified value as a double.
   *
   * @param value  Value to represent as a double value.
   *               May be null if this is a no-value.
   * @return       The requested double value.
   */
  public static double getAsDouble(Object value)
  {
    //
    // No-value
    //
    if (value == null)
      return Double.NaN;

    //
    // Number
    //
    if (value instanceof Number) {
      Number v = (Number) value;
      return v.doubleValue();
    }

    //
    // Date
    //
    if (value instanceof Date) {
      Date date = (Date) value;
      return (double) date.getTime();
    }

    //
    // Boolean
    //
    if (value instanceof Boolean) {
      boolean b = (Boolean) value;
      return b ? 1.0 : 0.0;
    }

    //
    // String
    //
    if (value instanceof String) {
      String v = ((String) value).trim();
      if (v.isEmpty())
        return Double.NaN;

      try {
        return Double.parseDouble(v);
      }
      catch (NumberFormatException exception) {
        // Ignore. It was possibly not meant to be
        // converted like this.
      }
    }

    //
    // Others
    //
    return (double) value.hashCode();
  }

  /**
   * Return the specified double value as an equivalent
   * object of the specified type.
   *
   * @param value      Value to convert. May be null.
   * @param valueType  Value type to convert to. Non-null.
   * @return           Object of type dataType. May be null.
   * @throws IllegalArgumentException  If valueType is null.
   */
  public static Object getAsType(double value, Class<?> valueType)
  {
    if (valueType == null)
      throw new IllegalArgumentException("valueType cannot be null");

    if (Double.isNaN(value))
      return null;

    if (valueType == Double.class)
      return value;

    if (valueType == Float.class)
      return (float) value;

    if (valueType == Long.class)
      return Math.round(value);

    if (valueType == Integer.class)
      return (int) Math.round(value);

    if (valueType == Date.class)
      return new Date((long) value);

    if (valueType == String.class)
      return "" + value;

    if (valueType == Boolean.class)
      return value != 0.0;

    if (valueType == Short.class)
      return (short) value;

    if (valueType == Byte.class)
      return (byte) value;

    // Others
    return null;
  }

  /**
   * Return the specified value as an object of the given type.
   *
   * @param value      Value to consider. Null if no-value.
   * @param valueType  Type to convert to. Non-null.
   * @return           The requested object. Null if no-value.
   * @throws IllegalArgumentException  If valueType is null.
   */
  public static Object getAsType(Object value, Class<?> valueType)
  {
    if (valueType == null)
      throw new IllegalArgumentException("valueType cannot be null");

    if (value == null)
      return null;

    // Not sure why we return emty string as null, but don't change until we know
    if (valueType == String.class)
      return value.toString().length() == 0 ? null : value.toString();

    if (valueType == Date.class && value instanceof String) {
      String dateString = value.toString();
      try {
        return ISO8601DateParser.parse(dateString);
      }
      catch (Exception exception) {
        return null;
      }
    }

    if (value.getClass() == valueType)
      return value;

    return getAsType(getAsDouble(value), valueType);
  }

  /**
   * Read a portion of the specified file so it can be used
   * for classifying the file by content.
   *
   * @param file    File to read from. Non-null.
   * @param nBytes  Number of bytes to read. About 2000 is fine for
   *                file classification.
   * @return        The portion read. Null if the read operation failed.
   */
  public static byte[] readContent(File file, int nBytes)
  {
    byte[] fileContent = null;

    FileChannel fileChannel = null;

    try {
      FileInputStream fileInputStream = new FileInputStream(file);
      fileChannel = fileInputStream.getChannel();
      ByteBuffer buffer = ByteBuffer.allocate(nBytes);
      int nActualBytes = fileChannel.read(buffer);
      if (nActualBytes > 0) {
        buffer.rewind();
        fileContent = new byte[nActualBytes];
        buffer.get(fileContent);
      }
    }
    catch (IOException exception) {
      return null;
    }
    finally {
      if (fileChannel != null) {
        try {
          fileChannel.close();
        }
        catch (IOException exception) {
          // Ignore
        }
      }
    }

    return fileContent;
  }

  /**
   * Testing this class.
   *
   * @param arguments  Application arguments. Not used.
   */
  private static void main(String[] arguments)
  {
    String s0 = "1 2 3 4";                  // split on ' ' gives "1","2","3","4"
    String s1 = "   1  2  3 4  ";           // split on ' ' gives "1","2","3","4"
    String s2 = "   \'1 2\' 3 \'4 \' \'\'"; // split on ' ' gives "1 2","3","4 ",""
    String s3 = "1 2 \'3\"\' 4";            // split on ' ' gives "1","2","3"","4"
    String s4 = " ";                        // split on ' ' gives nothing
    String s5 = ",";                        // split on ',' gives "",""
    String s6 = "1,2,3,4";                  // split on ',' gives "1","2","3","4"
    String s7 = "  1,  2,  3,  4 ";         // split on ',' gives "1","2","3","4"
    String s8 = ",,1,2,,3,4,5,,";           // split on ',' gives "","","1","2","","3","4","5","",""
    String s9 = "\'A B\',2";                // split on ',' gives "A B","2"

    List<String> t = Util.split(s9, ',');
    for (String a : t)
      System.out.println(a);
  }
}

