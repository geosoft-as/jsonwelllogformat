package no.geosoft.jwlf;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

/**
 * Class for writing JSON Well Log Format logs to disk.
 * <p>
 * Typical usage:
 * <blockquote>
 *   <pre>
 *   JsonWriter writer = new JsonWriter(new File("path/to/file.json"), true, 2);
 *   writer.write(jsonLog);
 *   writer.close();
 *   </pre>
 * </blockquote>
 *
 * If there is to much data to keep in memory, or the writing is based on a
 * streaming source, it is possible to append chunks of data to the last JsonLog
 * instance written, like:
 * <blockquote>
 *   <pre>
 *   JsonWriter writer = new JsonWriter(new File("path/to/file.json"), true, 2);
 *   writer.write(jsonLog);
 *   writer.append(jsonLog);
 *   writer.append(jsonLog);
 *   :
 *   writer.close();
 *   </pre>
 * </blockquote>
 *
 * Note that the pretty print mode of this writer will behave different than
 * a standard JSON writer in that it always writes curve data arrays horizontally,
 * with each curve vertically aligned.
 * <p>
 * If the JSON log header contains a valid <em>dataUri</em> property, the curve
 * data will be written in binary form to this location.
 *
 * @see <a href="https://jsonwelllogformat.org">https://jsonwelllogformat.org</a>
 *
 * @author <a href="mailto:jacob.dreyer@geosoft.no">Jacob Dreyer</a>
 */
public final class JsonWriter
  implements Closeable
{
  /** The logger instance. */
  private static final Logger logger_ = Logger.getLogger(JsonReader.class.getName());

  /** Platform independent new-line string. */
  private static String NEWLINE = System.getProperty("line.separator");

  /** The physical disk file to write. Null if writing directly to a stream. */
  private final File file_;

  /** The output stream to write. Null if writing to a file. */
  private final OutputStream outputStream_;

  /** True to write in human readable pretty format, false to write dense. */
  private final boolean isPretty_;

  /**
   * The new line token according to pretty print mode. Either NEWLINE or "".
   * Cached for efficiency.
   */
  private final String newline_;

  /**
   * Spacing between tokens according to pretty print mode. Either " " or "".
   * Cached for efficiency.
   */
  private final String spacing_;

  /** Current indentation according to pretty print mode. */
  private final Indentation indentation_;

  /** The writer instance. */
  private Writer writer_;

  /** Indicate if the last written JSON file contains data or not. */
  private boolean hasData_;

  /**
   * Create a JSON Well Log Format writer for the specified stream.
   *
   * @param outputStream  Stream to write. Non-null.
   * @param isPretty      True to write in human readable pretty format, false
   *                      to write as dense as possible.
   * @param indentation   The white space indentation used in pretty print mode. [0,&gt;.
   *                      If isPretty is false, this setting has no effect.
   * @throws IllegalArgumentException  If outputStream is null or indentation is out of bounds.
   */
  public JsonWriter(OutputStream outputStream, boolean isPretty, int indentation)
  {
    if (outputStream == null)
      throw new IllegalArgumentException("outputStream cannot be null");

    if (isPretty && indentation < 0)
      throw new IllegalArgumentException("Invalid indentation: " + indentation);

    file_ = null;
    outputStream_ = outputStream;
    isPretty_ = isPretty;
    newline_ = isPretty_ ? NEWLINE : "";
    spacing_ = isPretty_ ? " " : "";
    indentation_ = new Indentation(isPretty ? indentation : 0);
  }

  /**
   * Create a JSON Well Log Format writer for the specified disk file.
   *
   * @param file         Disk file to write. Non-null.
   * @param isPretty     True to write in human readable pretty format, false
   *                     to write as dense as possible.
   * @param indentation  The white space indentation used in pretty print mode. [0,&gt;.
   *                     If isPretty is false, this setting has no effect.
   * @throws IllegalArgumentException  If file is null or indentation is out of bounds.
   */
  public JsonWriter(File file, boolean isPretty, int indentation)
  {
    if (file == null)
      throw new IllegalArgumentException("file cannot be null");

    if (isPretty && indentation < 0)
      throw new IllegalArgumentException("Invalid indentation: " + indentation);

    file_ = file;
    outputStream_ = null;
    isPretty_ = isPretty;
    newline_ = isPretty_ ? NEWLINE : "";
    spacing_ = isPretty_ ? " " : "";
    indentation_ = new Indentation(isPretty ? indentation : 0);
  }

  /**
   * Create a JSON Well Log Format writer for the specified file.
   * Writing is done in pretty print mode with an indentation of 2.
   *
   * @param file  Disk file to write. Non-null.
   * @throws IllegalArgumentException  If file is null.
   */
  public JsonWriter(File file)
  {
    this(file, true, 2);
  }

  /**
   * Get the specified string token as a quoted text suitable for writing to
   * a JSON disk file, i.e. "null" if null, or properly escaped if non-null.
   *
   * @param value  Value to get as text. May be null.
   * @return       The value as a JSON text. Never null.
   */
  private static String getQuotedText(String value)
  {
    return value != null ? JsonUtil.encode(value) : "null";
  }

  /**
   * Compute the width of the widest element of the column of the specified curve.
   *
   * @param curve      Curve to compute column width of. Non-null.
   * @param formatter  Curve data formatter. Null if N/A for the specified curve.
   * @return           Width of widest element of the curve. [0,&gt;.
   */
  private static int computeColumnWidth(JsonCurve curve, Formatter formatter)
  {
    assert curve != null :  "curve cannot be null";

    int columnWidth = 0;
    Class<?> valueType = curve.getValueType();

    for (int index = 0; index < curve.getNValues(); index++) {
      for (int dimension = 0; dimension < curve.getNDimensions(); dimension++) {
        Object value = curve.getValue(dimension, index);

        String text;

        if (value == null)
          text = "null";

        else if (valueType == Date.class)
          text = "2018-10-10T12:20:00Z"; // Template

        else if (formatter != null)
          text = formatter.format(Util.getAsDouble(value));

        else if (valueType == String.class)
          text = getQuotedText(value.toString());

        else // Boolean and Integers
          text = value.toString();

        if (text.length() > columnWidth)
          columnWidth = text.length();
      }
    }

    return columnWidth;
  }

  /**
   * Get the specified data value as text, according to the specified value type,
   * the curve formatter, the curve width and the general rules for the JSON format.
   *
   * @param value      Curve value to get as text. May be null, in case "null" is returned.
   * @param valueType  Java value type of the curve of the value. Non-null.
   * @param formatter  Curve formatter. Specified for floating point values only, null otherwise,
   * @param width      Total width set aside for the values of this column. [0,&gt;.
   * @return           The JSON token to be written to file. Never null.
   */
  private String getText(Object value, Class<?> valueType, Formatter formatter, int width)
  {
    assert valueType != null : "valueType cannot be null";
    assert width >= 0 : "Invalid width: " + width;

    String text = null;

    if (value == null)
      text = "null";
    else if (valueType == Date.class)
      text = '\"' + ISO8601DateParser.toString((Date) value) + '\"';
    else if (valueType == Boolean.class)
      text = value.toString();
    else if (formatter != null)
      text = formatter.format(Util.getAsDouble(value));
    else if (Number.class.isAssignableFrom(valueType))
      text = value.toString();
    else if (valueType == String.class)
      text = getQuotedText(value.toString());
    else
      assert false : "Unrecognized valueType: " + valueType;

    String padding = isPretty_ ? Util.getSpaces(width - text.length()) : "";
    return padding + text;
  }

  /**
   * Write the specified JSON value to the current writer.
   *
   * @param jsonValue    Value to write. Non-null.
   * @param indentation  The current indentation level. Non-null.
   */
  private void writeValue(JsonValue jsonValue, Indentation indentation)
    throws IOException
  {
    assert jsonValue != null : "jsonValue cannot be null";
    assert indentation != null : "indentation cannot b3 null";

    switch (jsonValue.getValueType()) {
      case ARRAY :
        writeArray((JsonArray) jsonValue, indentation);
        break;

      case OBJECT :
        writeObject((JsonObject) jsonValue, indentation);
        break;

      case NUMBER :
        writer_.write(jsonValue.toString());
        break;

      case STRING :
        writer_.write(getQuotedText(((JsonString) jsonValue).getString()));
        break;

      case FALSE :
        writer_.write("false");
        break;

      case TRUE :
        writer_.write("true");
        break;

      case NULL :
        writer_.write("null");
        break;

      default :
        assert false : "Unrecognized value type: " + jsonValue.getValueType();
    }
  }

  /**
   * Write the specified JSON object to the current writer.
   *
   * @param jsonObject   Object to write. Non-null.
   * @param indentation  The current indentation level. Non-null.
   */
  private void writeObject(JsonObject jsonObject, Indentation indentation)
    throws IOException
  {
    assert jsonObject != null : "jsonObject cannot be null";
    assert indentation != null : "indentation cannot be null";

    writer_.write('{');

    boolean isFirst = true;

    for (Map.Entry<String,JsonValue> entry : jsonObject.entrySet()) {
      String key = entry.getKey();
      JsonValue value = entry.getValue();

      if (!isFirst)
        writer_.write(',');

      writer_.write(newline_);
      writer_.write(indentation.push().toString());
      writer_.write('\"');
      writer_.write(key);
      writer_.write('\"');
      writer_.write(':');
      writer_.write(spacing_);

      writeValue(value, indentation.push());

      isFirst = false;
    }

    if (!jsonObject.isEmpty()) {
      writer_.write(newline_);
      writer_.write(indentation.toString());
    }

    writer_.write("}");
  }

  /**
   * Write the specified JSON array to the current writer.
   *
   * @param jsonArray    Array to write. Non-null.
   * @param indentation  The current indentation level. Non-null.
   */
  private void writeArray(JsonArray jsonArray, Indentation indentation)
    throws IOException
  {
    assert jsonArray != null : "jsonArray cannot be null";
    assert indentation != null : "indentation cannot be null";

    boolean isHorizontal = !JsonUtil.containsObjects(jsonArray);

    writer_.write('[');

    boolean isFirst = true;

    for (JsonValue jsonValue : jsonArray) {
      if (!isFirst) {
        writer_.write(",");
        if (isHorizontal)
          writer_.write(spacing_);
      }

      if (!isHorizontal) {
        writer_.write(newline_);
        writer_.write(indentation.push().toString());
      }

      writeValue(jsonValue, indentation.push());

      isFirst = false;
    }

    if (!jsonArray.isEmpty() && !isHorizontal) {
      writer_.write(newline_);
      writer_.write(indentation.toString());
    }

    writer_.write(']');
  }

  /**
   * Write the speicfied JSON Well Logg Format header object to the current writer.
   * <p>
   * This method is equal the writeHeader method apart from its special handling
   * of the specific keys startIndex, endIndex and step so that these gets identical
   * formatting as the index curve of the log.
   *
   * @param header       The JSON Well Log Format header object. Non-null.
   * @param indentation  The current indentation level. Non-null.
   * @param log          The log of this header. Non-null.
   */
  private void writeHeaderObject(JsonObject header, Indentation indentation, JsonLog log)
    throws IOException
  {
    assert header != null : "header cannot be null";
    assert indentation != null : "indentation cannot be null";
    assert log != null : "log cannot be null";

    JsonCurve indexCurve = log.getNCurves() > 0 ? log.getCurves().get(0) : null;
    Formatter indexCurveFormatter = indexCurve != null ? log.createFormatter(indexCurve, true) : null;

    writer_.write('{');

    boolean isFirst = true;

    for (Map.Entry<String,JsonValue> entry : header.entrySet()) {
      String key = entry.getKey();
      JsonValue value = entry.getValue();

      if (!isFirst)
        writer_.write(',');

      writer_.write(newline_);
      writer_.write(indentation.push().toString());
      writer_.write('\"');
      writer_.write(key);
      writer_.write('\"');
      writer_.write(':');
      writer_.write(spacing_);

      //
      // Special handling of startIndex, endIndex and step so that
      // they get the same formatting as the index curve data.
      //
      if (indexCurveFormatter != null && (key.equals("startIndex") || key.equals("endIndex") || key.equals("step"))) {
        double v = Util.getAsDouble(JsonUtil.getValue(value));
        String text = Double.isFinite(v) ? indexCurveFormatter.format(v) : "null";
        writer_.write(text);
      }
      else if (value.getValueType() == JsonValue.ValueType.OBJECT && JsonTable.isTable((JsonObject) value)) {
        writeObject((JsonObject) value, indentation.push());
      }
      else {
        writeValue(value, indentation.push());
      }

      isFirst = false;
    }

    if (!header.isEmpty()) {
      writer_.write(newline_);
      writer_.write(indentation.toString());
    }

    writer_.write("}");
  }

  /**
   * Write the curve data of the specified log to the given file
   * in binary form.
   *
   * @param log   Log of data to write. Non-null.
   * @param file  File to write to. Non-null.
   */
  private void writeDataAsBinary(JsonLog log, File file)
    throws IOException
  {
    FileOutputStream fileOutputStream = new FileOutputStream(file);

    DataOutputStream outputStream = new DataOutputStream(new BufferedOutputStream(fileOutputStream));

    try {
      for (int index = 0; index < log.getNValues(); index++) {
        for (JsonCurve curve : log.getCurves()) {
          Class<?> valueType = curve.getValueType();
          int size = curve.getSize();
          for (int dimension = 0; dimension < curve.getNDimensions(); dimension++) {
            Object value = curve.getValue(dimension, index);

            //
            // Double
            //
            if (valueType == Double.class) {
              double v = value != null ? (Double) value : Double.NaN;
              outputStream.writeDouble(v);
            }

            //
            // Long
            //
            if (valueType == Long.class) {
              long v = value != null ? (Long) value : Long.MAX_VALUE;
              outputStream.writeLong(v);
            }

            //
            // String
            //
            if (valueType == String.class) {
              byte[] bytes = Util.toUtf8(value, size);
              outputStream.write(bytes, 0, bytes.length);
            }

            //
            // Boolean
            //
            if (valueType == Boolean.class) {
              int v = value != null ? ((Boolean) value) ? 1 : 0 : 255;
              outputStream.writeByte(v);
            }

            //
            // Date
            //
            if (valueType == Date.class) {
              size = 30;
              Date date = value != null ? (Date) value : null;
              String v = date != null ? ISO8601DateParser.toString(date) : "";
              String s = Util.toString(v, size);
              byte[] bytes = s.getBytes(StandardCharsets.US_ASCII);
              outputStream.write(bytes, 0, bytes.length);
            }
          }
        }
      }
    }
    catch (IOException exception) {
      throw exception;
    }
    finally {
      outputStream.close();
    }
  }

  /**
   * Write the curve data of the specified log.
   *
   * @param log   Log of data to write. Non-null.
   * @throws IOException  If the write operation fails for some reason.
   */
  private void writeDataAsText(JsonLog log)
    throws IOException
  {
    assert log != null : "log cannot be null";

    Indentation indentation = indentation_.push().push().push();

    List<JsonCurve> curves = log.getCurves();

    // Create formatters for each curve
    Map<JsonCurve,Formatter> formatters = new HashMap<>();
    for (int curveNo = 0; curveNo < log.getNCurves(); curveNo++) {
      JsonCurve curve = curves.get(curveNo);
      Formatter formatter = log.createFormatter(curve, curveNo == 0);
      formatters.put(curve, formatter);
    }

    // Compute column width for each data column
    Map<JsonCurve,Integer> columnWidths = new HashMap<>();
    for (JsonCurve curve : curves)
      columnWidths.put(curve, computeColumnWidth(curve, formatters.get(curve)));

    for (int index = 0; index < log.getNValues(); index++) {
      for (int curveNo = 0; curveNo < log.getNCurves(); curveNo++) {
        JsonCurve curve = curves.get(curveNo);
        Class<?> valueType = curve.getValueType();
        int nDimensions = curve.getNDimensions();
        int width = columnWidths.get(curve);
        Formatter formatter = formatters.get(curve);

        if (curveNo == 0) {
          writer_.write(indentation.toString());
          writer_.write('[');
        }

        if (nDimensions > 1) {
          if (curveNo > 0) {
            writer_.write(',');
            writer_.write(spacing_);
          }

          writer_.write('[');
          for (int dimension = 0; dimension < nDimensions; dimension ++) {
            Object value = curve.getValue(dimension, index);
            String text = getText(value, valueType, formatter, width);

            if (dimension > 0) {
              writer_.write(',');
              writer_.write(spacing_);
            }

            writer_.write(text);
          }
          writer_.write(']');
        }
        else {
          Object value = curve.getValue(0, index);
          String text = getText(value, valueType, formatter, width);

          if (curveNo > 0) {
            writer_.write(',');
            writer_.write(spacing_);
          }

          writer_.write(text);
        }
      }

      writer_.write(']');
      if (index < log.getNValues() - 1) {
        writer_.write(',');
        writer_.write(newline_);
      }
    }
  }

  /**
   * Write the curve data of the specified JSON log.
   *
   * @param log           Log to write curve data of. Non-null.
   * @throws IOException  If the write operation fails for some reason.
   */
  private void writeData(JsonLog log)
    throws IOException
  {
    String dataUri = log.getDataUri();

    //
    // Case 1: Write data as JSON text in same stream
    //
    if (dataUri == null) {
      writeDataAsText(log);
    }

    //
    // Case 2: Write data as binary in separate file
    //
    if (dataUri != null) {
      try {
        URI uri = new URI(dataUri);

        // Can only refer to a relative URI if source is a file
        if (!uri.isAbsolute() && file_ != null)
          uri = file_.toURI().resolve(uri);

        File dataFile = new File(uri);

        writeDataAsBinary(log, dataFile);
      }
      catch (URISyntaxException exception) {
        logger_.log(Level.SEVERE, "Unable to write binary data to " + dataUri, exception);
      }
    }
  }

  /**
   * Write the specified log.
   * <p>
   * Multiple logs can be written in sequence to the same stream.
   * Additional data can be appended to the last one by {@link #append}.
   * When writing is done, close the writer with {@link #close}.
   * <p>
   * If the log header contains a valid <em>dataUri</em> property, the curve
   * data will be written in binary form to this location.
   *
   * @param log  Log to write. Non-null.
   * @throws IllegalArgumentException  If log is null.
   * @throws IOException  If the write operation fails for some reason.
   */
  public void write(JsonLog log)
    throws IOException
  {
    if (log == null)
      throw new IllegalArgumentException("log cannot be null");

    boolean isFirstLog = writer_ == null;

    // Create the writer on first write operation
    if (isFirstLog) {
      OutputStream outputStream = file_ != null ? new FileOutputStream(file_) : outputStream_;
      writer_ = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
      writer_.write('[');
      writer_.write(newline_);
    }

    // If this is an additional log, close the previous and make ready for a new
    else {
      writer_.write(newline_);
      writer_.write(indentation_.push().push().toString());
      writer_.write(']');
      writer_.write(newline_);

      writer_.write(indentation_.push().toString());
      writer_.write("},");
      writer_.write(newline_);
    }

    Indentation indentation = indentation_.push();

    writer_.write(indentation.toString());
    writer_.write('{');
    writer_.write(newline_);

    indentation = indentation.push();

    //
    // "header"
    //
    writer_.write(indentation.toString());
    writer_.write("\"header\":");
    writer_.write(spacing_);

    writeHeaderObject(log.getHeader(), indentation, log);

    writer_.write(',');

    //
    // "curves"
    //
    writer_.write(newline_);
    writer_.write(indentation.toString());
    writer_.write("\"curves\": [");

    boolean isFirstCurve = true;

    List<JsonCurve> curves = log.getCurves();

    for (JsonCurve curve : curves) {

      if (!isFirstCurve)
        writer_.write(',');

      writer_.write(newline_);
      indentation = indentation.push();
      writer_.write(indentation.toString());
      writer_.write('{');
      writer_.write(newline_);
      indentation = indentation.push();

      // Name
      writer_.write(indentation.toString());
      writer_.write("\"name\":");
      writer_.write(spacing_);
      writer_.write(getQuotedText(curve.getName()));
      writer_.write(',');
      writer_.write(newline_);

      // Description
      writer_.write(indentation.toString());
      writer_.write("\"description\":");
      writer_.write(spacing_);
      writer_.write(getQuotedText(curve.getDescription()));
      writer_.write(',');
      writer_.write(newline_);

      // Quantity
      writer_.write(indentation.toString());
      writer_.write("\"quantity\":");
      writer_.write(spacing_);
      writer_.write(getQuotedText(curve.getQuantity()));
      writer_.write(',');
      writer_.write(newline_);

      // Unit
      writer_.write(indentation.toString());
      writer_.write("\"unit\":");
      writer_.write(spacing_);
      writer_.write(getQuotedText(curve.getUnit()));
      writer_.write(',');
      writer_.write(newline_);

      // Value type
      writer_.write(indentation.toString());
      writer_.write("\"valueType\":");
      writer_.write(spacing_);
      writer_.write(getQuotedText(JsonValueType.get(curve.getValueType()).toString()));
      writer_.write(',');
      writer_.write(newline_);

      // Max size
      if (curve.getValueType() == String.class) {
        writer_.write(indentation.toString());
        writer_.write("\"maxSize\":");
        writer_.write(spacing_);
        writer_.write("" + curve.getSize());
        writer_.write(',');
        writer_.write(newline_);
      }

      // Dimension
      writer_.write(indentation.toString());
      writer_.write("\"dimensions\":");
      writer_.write(spacing_);
      writer_.write("" + curve.getNDimensions());
      writer_.write(newline_);

      indentation = indentation.pop();
      writer_.write(indentation.toString());
      writer_.write('}');
      indentation = indentation.pop();

      isFirstCurve = false;
    }

    writer_.write(newline_);
    writer_.write(indentation.toString());
    writer_.write(']');

    //
    // "data"
    //
    writer_.write(',');
    writer_.write(newline_);
    writer_.write(indentation.toString());
    writer_.write("\"data\": [");
    writer_.write(newline_);

    writeData(log);

    hasData_ = log.getNValues() > 0;
  }

  /**
   * Append the curve data of the specified log.
   * <p>
   * This feature can be used to <em>stream</em> data to a JSON
   * destination. By repeatedly clearing and populating the log
   * curves with new data there is no need for the client to
   * keep the full volume in memory at any point in time.
   * <p>
   * If the log hedaer contains a valid <em>dataUri</em> property, the curve
   * data will be written in binary form to this location.
   * <p>
   * <b>NOTE:</b> This method should be called after the
   * JSON Well Log Format metadata has been written (see {@link #write}),
   * and the JSON log must be compatible with this.
   * <p>
   * When writing is done, close the stream with {@link #close}.
   *
   * @param log   Log to append to stream. Non-null.
   * @throws IllegalArgumentException  If log is null.
   * @throws IllegalStateException     If the writer is not open for writing.
   * @throws IOException  If the write operation fails for some reason.
   */
  public void append(JsonLog log)
    throws IOException
  {
    if (log == null)
      throw new IllegalArgumentException("log cannot be null");

    if (writer_ == null)
      throw new IllegalStateException("Writer is not open");

    if (hasData_) {
      writer_.write(',');
      writer_.write(newline_);
    }

    writer_.write(indentation_.toString());
    writeData(log);

    if (!hasData_ && log.getNValues() > 0)
      hasData_ = true;
  }

  /**
   * Append closing brackets and close the back-end stream.
   */
  @Override
  public void close()
    throws IOException
  {
    // Nothing to do if the writer was never opened
    if (writer_ == null)
      return;

    // Complete the data array
    writer_.write(newline_);
    writer_.write(indentation_.push().push().toString());
    writer_.write(']');
    writer_.write(newline_);

    // Complete the log object
    writer_.write(indentation_.push().toString());
    writer_.write('}');
    writer_.write(newline_);

    // Complete the logs array
    writer_.write(']');
    writer_.write(newline_);

    writer_.close();
    writer_ = null;
  }

  /**
   * Convenience method for returning a string representation of the specified logs.
   * <p>
   * <b>Note: </b>If a log header contains the <em>dataUri</em> property, this
   * will be masked for the present operation so that curve data always appears
   * in the returned JSON string.
   *
   * @param logs         Logs to write. Non-null.
   * @param isPretty     True to write in human readable pretty format, false
   *                     to write as dense as possible.
   * @param indentation  The white space indentation used in pretty print mode. [0,&gt;.
   *                     If isPretty is false, this setting has no effect.
   * @return             The requested string. Never null.
   * @throws IllegalArgumentException  If logs is null or indentation is out of bounds.
   */
  public static String toString(List<JsonLog> logs, boolean isPretty, int indentation)
  {
    if (logs == null)
      throw new IllegalArgumentException("logs cannot be null");

    if (indentation < 0)
      throw new IllegalArgumentException("invalid indentation: " + indentation);

    ByteArrayOutputStream stringStream = new ByteArrayOutputStream();
    JsonWriter writer = new JsonWriter(stringStream, isPretty, indentation);

    String string = "";

    try {
      for (JsonLog log : logs) {

        // Temporarily hide the dataUri property or the data will be written
        // to binary file
        String dataUri = log.getDataUri();
        if (dataUri != null)
          log.setDataUri(null);

        writer.write(log);

        // Restore dataUri
        if (dataUri != null)
          log.setDataUri(dataUri);
      }
    }
    catch (IOException exception) {
      // Since we are writing to memory (ByteArrayOutputStream) we don't really
      // expect an IOException so if we get one anyway, we are in serious trouble
      throw new RuntimeException("Unable to write", exception);
    }
    finally {
      try {
        writer.close();
        string = new String(stringStream.toByteArray(), StandardCharsets.UTF_8);
      }
      catch (IOException exception) {
        // Again: This will never happen.
        throw new RuntimeException("Unable to write", exception);
      }
    }

    return string;
  }

  /**
   * Convenience method for returning a string representation of the specified log.
   * <p>
   * <b>Note: </b>If a log header contains the <em>dataUri</em> property, this
   * will be masked for the present operation so that curve data always appears
   * in the returned JSON string.
   *
   * @param log          Log to write. Non-null.
   * @param isPretty     True to write in human readable pretty format, false
   *                     to write as dense as possible.
   * @param indentation  The white space indentation used in pretty print mode. [0,&gt;.
   *                     If isPretty is false, this setting has no effect.
   * @return             The requested string. Never null.
   * @throws IllegalArgumentException  If log is null or indentation is out of bounds.
   */
  public static String toString(JsonLog log, boolean isPretty, int indentation)
  {
    if (log == null)
      throw new IllegalArgumentException("log cannot be null");

    if (indentation < 0)
      throw new IllegalArgumentException("invalid indentation: " + indentation);

    List<JsonLog> logs = new ArrayList<>();
    logs.add(log);

    return toString(logs, isPretty, indentation);
  }

  /**
   * Convenience method for returning a pretty printed string representation
   * of the specified log.
   * <p>
   * <b>Note: </b>If a log header contains the <em>dataUri</em> property, this
   * will be masked for the present operation so that curve data always appears
   * in the returned JSON string.
   *
   * @param log  Log to write. Non-null.
   * @return     The requested string. Never null.
   * @throws IllegalArgumentException  If log is null.
   */
  public static String toString(JsonLog log)
  {
    if (log == null)
      throw new IllegalArgumentException("log cannot be null");

    return toString(log, true, 2);
  }
}
