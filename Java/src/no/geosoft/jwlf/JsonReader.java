package no.geosoft.jwlf;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.stream.JsonParser;

/**
 * Class for reading JSON Well Log Format files.
 * <p>
 * Typical usage:
 * <blockquote>
 *   <pre>
 *   JsonReader reader = new JsonReader(new File("path/to/file.json"));
 *   List&lt;JsonLog&gt; logs = reader.read(true, true, null);
 *   </pre>
 * </blockquote>
 *
 * If the curve data is not needed, it is possible to read only the
 * metadata. The curve data may be filled in later:
 *
 * <blockquote>
 *   <pre>
 *   JsonReader reader = new JsonReader(new File("path/to/file.json"));
 *   List&lt;JsonLog&gt; logs = reader.read(false, false, null);
 *   :
 *   reader.readData(logs);
 *   </pre>
 * </blockquote>
 *
 * Note that even if only metadata is read, all curve information
 * are properly established as this information comes from metadata.
 * Only the curve <em>values</em> will be missing.
 * <p>
 * If the JSON content is larger than physical memory, it is possible
 * to <em>stream</em> (process than throw away) the data during read.
 * See {@link JsonDataListener}. The same mechanism may be used
 * to <em>abort</em> the reading process during the operation.
 *
 * @see <a href="https://jsonwelllogformat.org">https://jsonwelllogformat.org</a>
 *
 * @author <a href="mailto:jacob.dreyer@geosoft.no">Jacob Dreyer</a>
 */
public final class JsonReader
{
  /** The logger instance. */
  private static final Logger logger_ = Logger.getLogger(JsonReader.class.getName());

  /** The file to read. Null if reading from stream or JSON array. */
  private final File file_;

  /** The stream to be read. Null if reading from file or JSON array. */
  private final InputStream inputStream_;

  /** The JSON array holding the log content. Null if reading from file or stream. */
  private final JsonArray jsonArray_;

  /** Temporary storage for "data" in case these appears before "curves" in the stream. */
  private TemporaryStorage temporaryStorage_ = null;

  /**
   * Create a JSON reader for the specified stream.
   *
   * @param inputStream  Stream to read. Non-null.
   * @throws IllegalArgumentException  If inputStream is null.
   */
  public JsonReader(InputStream inputStream)
  {
    if (inputStream == null)
      throw new IllegalArgumentException("inputStream cannot be null");

    file_ = null;
    inputStream_ = inputStream;
    jsonArray_ = null;
  }

  /**
   * Create a JSON reader for the specified disk file.
   *
   * @param file  Disk file to read. Non-null.
   * @throws IllegalArgumentException  If file is null.
   */
  public JsonReader(File file)
  {
    if (file == null)
      throw new IllegalArgumentException("file cannot be null");

    file_ = file;
    inputStream_ = null;
    jsonArray_ = null;
  }

  /**
   * Create a JSON reader for the specified text.
   *
   * @param text  Text to read. Non-null.
   * @throws IllegalArgumentException  If text is null.
   */
  public JsonReader(String text)
  {
    if (text == null)
      throw new IllegalArgumentException("text cannot be null");

    file_ = null;
    jsonArray_ = null;

    // NOTE: This stream will never be closed.
    // This is not a problem as it is all in memory and close() is anyway empty.
    inputStream_ = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Create a JSON reader from a populated JSON array of JsonLogs.
   *
   * @param jsonArray  Array constituting the JSON Well Log Format logs. Non-null.
   * @throws IllegalArgumentException  If jsonArray is null.
   */
  public JsonReader(JsonArray jsonArray)
  {
    if (jsonArray == null)
      throw new IllegalArgumentException("jsonArray cannot be null");

    file_ = null;
    inputStream_ = null;
    jsonArray_ = jsonArray;
  }

  /**
   * Get the probability that the specified sequence of bytes is
   * from a JSON Well Log Format file.
   *
   * @param content  A number of bytes from the start of a file,
   *                 typically 2-3000. May be null, in case 0.0 is returned.
   * @return  Probability that the sequence is from a JSON Well Log Format
   *          file. [0.0,1.0].
   * @see #isJsonFile
   */
  private static double isJson(byte[] content)
  {
    if (content == null)
      return 0.0;

    String s = new String(content);

    if (!s.contains("\"header\""))
      return 0.05;

    if (!s.contains("["))
      return 0.05;

    if (!s.contains(":"))
      return 0.05;

    // Curves may be far into the stream so we can't conclude on it
    if (s.contains("\"curves\""))
      return 0.95;

    // TODO: More tests here

    return 0.75;
  }

  /**
   * Get the probability that the specified file is a
   * JSON Well Log Format file.
   * <p>
   * The check can be done with or without considering the
   * <em>content</em> of the file. In the latter case, only
   * the file name (typically its extension) is considered.
   * In the former case a portion from the start of the file
   * is used to match for known patterns. By passing the
   * portion as an argument, the client code can read this
   * <em>once</em> and then pass it to different classifiers
   * in order to determine its most likely type.
   * <p>
   * Getting a portion of a file can be done by:
   * <pre>
   *   File file = ...;
   *   BufferedInputStream stream = new BufferedInputStream(new FileInputStream(file));
   *   byte[] content = new byte[2000];
   *   stream.read(content, 0, content.length);
   *   stream.close();
   * </pre>
   *
   * @param file     File to check. Null to classify on content only.
   * @param content  A number of bytes from the start of the file.
   *                 Null to classify on file name only.
   * @return  Probability that the file is a JSON Well Log file. [0.0,1.0].
   */
  public static double isJsonFile(File file, byte[] content)
  {
    if (file == null)
      return isJson(content);

    if (file.isDirectory())
      return 0.0;

    if (!file.exists())
      return 0.0;

    boolean isFileNameMatching = file.getName().toLowerCase(Locale.US).endsWith(".json");
    double contentMatch = isJson(content);

    if (isFileNameMatching && content == null)
      return 0.75; // File name is matching, content is not considered
    else if (content != null)
      return contentMatch;
    else
      return 0.2;
  }

  /**
   * Get name of the source being read.
   *
   * @return  The source being read. Never null.
   */
  private String getSource()
  {
    if (file_ != null)
      return file_.getPath();

    if (inputStream_ != null)
      return inputStream_.toString();

    return jsonArray_.toString();
  }

  /**
   * Read binary curve data from the specified file and populate
   * the given log instance.
   *
   * @param log           Log to populate with curve data. Non-null.
   * @param binaryFile    File to read from. Non-null.
   * @param dataListener  Listener that will be notified when new data has read.
   */
  private void readBinaryData(JsonLog log, File binaryFile, JsonDataListener dataListener)
    throws InterruptedException, IOException
  {
    DataInputStream inputStream = null;

    try {
      inputStream = new DataInputStream(new FileInputStream(binaryFile));

      while (inputStream.available() > 0) {

        for (JsonCurve curve : log.getCurves()) {
          Class<?> valueType = curve.getValueType();
          int stringSize = valueType == Date.class ? 30 : valueType == String.class ? curve.getSize() : 0;
          for (int dimension = 0; dimension < curve.getNDimensions(); dimension++) {
            Object value = null;

            //
            // Double
            //
            if (valueType == Double.class || valueType == Float.class) {
              double v = inputStream.readDouble();
              value = Double.isNaN(v) ? null : v;
            }

            //
            // Long
            //
            else if (valueType == Long.class || valueType == Integer.class || valueType == Short.class || valueType == Byte.class) {
              long v = inputStream.readLong();
              value = v == Long.MAX_VALUE ? null : v;
            }

            //
            // Boolean
            //
            else if (valueType == Boolean.class) {
              byte v = inputStream.readByte();
              value = v == 1 ? Boolean.TRUE : v == 0 ? Boolean.FALSE : null;
            }

            //
            // String
            //
            else if (valueType == String.class) {
              byte[] b = new byte[stringSize];
              inputStream.read(b);
              String text = new String(b, StandardCharsets.UTF_8);
              value = text.trim();
            }

            //
            // Date
            //
            else if (valueType == Date.class) {
              byte[] b = new byte[stringSize];
              inputStream.read(b);
              String text = new String(b, StandardCharsets.US_ASCII);
              try {
                value = ISO8601DateParser.parse(text);
              }
              catch (ParseException exception) {
                logger_.log(Level.WARNING, "Invalid date format: " + text);
              }
            }

            else {
              assert false : "Programming error";
            }

            curve.addValue(dimension, value);
          }

          if (dataListener != null) {
            boolean shouldContinue = dataListener.dataRead(log);
            if (!shouldContinue)
              throw new InterruptedException("Reading aborted by client: " + getSource());
          }
        }
      }
    }
    catch (IOException exception) {
      throw exception;
    }
    finally {
      if (inputStream != null)
        inputStream.close();
    }
  }

  /**
   * Read curve data from the current location in the JSON parser.
   *
   * @param jsonParser               The JSON parser. Non-null.
   * @param log                      The log to populate with data. Non-null.
   * @param shouldReadBulkData       True if bulk data should be stored, false if not.
   * @param dataListener             Listener that will be notified when new data has
   *                                 been read. Null if not used.
   * @throws InterruptedException    If the read operation was interrupted by the client
   *                                 through the data listener.
   */
  private void readData(JsonParser jsonParser, JsonLog log,
                        boolean shouldReadBulkData,
                        JsonDataListener dataListener)
    throws InterruptedException
  {
    assert jsonParser != null : "jsonParser cannot be null";
    assert log != null : "log cannot be null";

    int curveNo = 0;
    int dimension = 0;

    int level = 0;

    while (jsonParser.hasNext()) {
      JsonParser.Event parseEvent = jsonParser.next();

      //
      // Case 1: "["
      //
      if (parseEvent == JsonParser.Event.START_ARRAY) {
        level++;
        dimension = 0;
      }

      //
      // Case 2: "]"
      //
      else if (parseEvent == JsonParser.Event.END_ARRAY) {
        level--;
        dimension = 0;

        // If we get to level 0 we are all done
        if (level == 0)
          return;

        // If at level 1 we have reached end of one row
        if (level == 1) {
          curveNo = 0;
          dimension = 0;

          if (dataListener != null) {
            boolean shouldContinue = dataListener.dataRead(log);
            if (!shouldContinue)
              throw new InterruptedException("Reading aborted by client: " + getSource());
          }
        }

        // Otherwise we have reached the end of a n-dim curve
        else {
          curveNo++;
          dimension = 0;
        }
      }

      //
      // Case 3: Invalid
      //
      else if (parseEvent == JsonParser.Event.START_OBJECT ||
               parseEvent == JsonParser.Event.END_OBJECT ||
               parseEvent == JsonParser.Event.KEY_NAME) {
        logger_.log(Level.SEVERE, "Unrecognized event in curve data: " + parseEvent + ". Aborting.");
        return;
      }

      //
      // Case 4: data value
      //
      else if (shouldReadBulkData) {
        Object value = null;
        if (parseEvent == JsonParser.Event.VALUE_NUMBER)
          value = jsonParser.getBigDecimal().doubleValue();
        else if (parseEvent == JsonParser.Event.VALUE_STRING)
          value = jsonParser.getString();
        else if (parseEvent == JsonParser.Event.VALUE_TRUE)
          value = Boolean.TRUE;
        else if (parseEvent == JsonParser.Event.VALUE_FALSE)
          value = Boolean.FALSE;

        if (temporaryStorage_ == null) {
          JsonCurve curve = log.getCurves().get(curveNo);

          if (shouldReadBulkData)
            curve.addValue(dimension, value);
        }
        else {
          temporaryStorage_.add(curveNo, dimension, value);
        }

        if (level == 2)
          curveNo++;
        else
          dimension++;
      }
    }

    assert false : "Invalid state";
  }


  /**
   * Read a curve definition from the current location of the specified
   * JSON parser.
   *
   * @param jsonParser  The JSON parser. Non-null.
   * @return  The curve instance. Null if there is not adequate information to
   *          define a curve.
   */
  private static JsonCurve readCurveDefinition(JsonParser jsonParser)
  {
    assert jsonParser != null : "jsonParser cannot be null";

    String curveName = null;
    String description = null;
    String quantity = null;
    String unit = null;
    Class<?> valueType = null;
    int nDimensions = 1;
    int size = 0;

    while (jsonParser.hasNext()) {
      JsonParser.Event parseEvent = jsonParser.next();

      if (parseEvent == JsonParser.Event.END_OBJECT) {
        if (curveName == null) {
          logger_.log(Level.WARNING, "Curve name is missing. Skip curve.");
          return null;
        }

        if (valueType == null) {
          logger_.log(Level.WARNING, "Curve value type is missing. Skip curve.");
          return null;
        }

        JsonCurve curve = new JsonCurve(curveName, description,
                                        quantity, unit, valueType,
                                        nDimensions);
        if (valueType == String.class)
          curve.setSize(size);

        return curve;
      }

      if (parseEvent == JsonParser.Event.KEY_NAME) {
        String key = jsonParser.getString();

        //
        // "name"
        //
        if (key.equals("name")) {
          jsonParser.next();
          curveName = jsonParser.getString();
        }

        //
        // "description"
        //
        else if (key.equals("description")) {
          parseEvent = jsonParser.next();
          description = parseEvent == JsonParser.Event.VALUE_STRING ? jsonParser.getString() : null;
        }

        //
        // "quantity"
        //
        else if (key.equals("quantity")) {
          parseEvent = jsonParser.next();
          quantity = parseEvent == JsonParser.Event.VALUE_STRING ? jsonParser.getString() : null;
        }

        //
        // "unit"
        //
        else if (key.equals("unit")) {
          parseEvent = jsonParser.next();
          unit = parseEvent == JsonParser.Event.VALUE_STRING ? jsonParser.getString() : null;
        }

        //
        // "valueType"
        //
        else if (key.equals("valueType")) {
          jsonParser.next();
          String valueTypeString = jsonParser.getString();
          JsonValueType jsonValueType = JsonValueType.get(valueTypeString);
          if (jsonValueType == null)
            logger_.log(Level.WARNING, "Unrecognized value type: " + valueTypeString + ". Using float instead.");
          valueType = jsonValueType != null ? jsonValueType.getValueType() : Double.class;
        }

        //
        // "dimensions"
        //
        else if (key.equals("dimensions")) {
          jsonParser.next();
          nDimensions = jsonParser.getInt();
        }

        //
        // "maxSize"
        //
        else if (key.equals("maxSize")) {
          jsonParser.next();
          size = jsonParser.getInt();
          if (size < 0) {
            logger_.log(Level.WARNING, "Invalid size: " + size + ". Ignoring.");
            size = 0;
          }
        }
      }
    }

    return null;
  }

  /**
   * Read the curves information from the current location of the JSON parser.
   *
   * @param jsonParser  The JSON parser. Non-null.
   * @param log         The log to populate. Non-null.
   */
  private static void readCurveDefinitions(JsonParser jsonParser, JsonLog log)
  {
    assert jsonParser != null : "jsonParser cannot be null";
    assert log != null : "log cannot be null";

    while (jsonParser.hasNext()) {
      JsonParser.Event parseEvent = jsonParser.next();

      if (parseEvent == JsonParser.Event.END_ARRAY)
        return;

      if (parseEvent == JsonParser.Event.START_OBJECT) {
        JsonCurve curve = readCurveDefinition(jsonParser);
        if (curve != null)
          log.addCurve(curve);
      }
    }
  }

  /**
   * Read log object from the current position in the JSON parser
   * and return as a JsonLog instance.
   *
   * @param jsonParser           The parser. Non-null.
   * @param shouldReadBulkData   True if bulk data should be read, false
   *                             if only metadata should be read.
   * @param dataListener         Client data listener. Null if not used.
   * @return  The read instance. Never null.
   * @throws IOException  If the read operation fails for some reason.
   * @throws InterruptedException  If the client returns <tt>false</tt> from
   *                             the {@link JsonDataListener#dataRead} method.
   */
  private JsonLog readLog(JsonParser jsonParser,
                          boolean shouldReadBulkData,
                          JsonDataListener dataListener)
    throws IOException, InterruptedException
  {
    JsonLog log = new JsonLog();

    while (jsonParser.hasNext()) {
      JsonParser.Event parseEvent = jsonParser.next();

      if (parseEvent == JsonParser.Event.END_OBJECT) {

        //
        // If there is a separate data file, read it now
        //
        String dataUri = log.getDataUri();
        if (dataUri != null && shouldReadBulkData) {
          try {
            URI uri = new URI(dataUri);

            // Can only refer to a relative URI if source is a file
            if (!uri.isAbsolute() && file_ != null)
              uri = file_.toURI().resolve(uri);

            File dataFile = new File(uri);

            readBinaryData(log, dataFile, dataListener);
          }
          catch (URISyntaxException exception) {
            logger_.log(Level.WARNING, "Invalid URI: " + dataUri, exception);
          }
          catch (IOException exception) {
            logger_.log(Level.WARNING, "Unable to read binary data from " + dataUri, exception);
          }
        }

        log.trimCurves();

        return log;
      }

      if (parseEvent == JsonParser.Event.KEY_NAME) {
        String key = jsonParser.getString();

        //
        // "header"
        //
        if (key.equals("header")) {
          JsonObjectBuilder objectBuilder = JsonUtil.readJsonObject(jsonParser);
          JsonObject header = objectBuilder.build();
          log.setHeader(header);
        }

        //
        // "curves"
        //
        if (key.equals("curves")) {
          readCurveDefinitions(jsonParser, log);

          // If "data" was read before "curves" we move data from temporary storage
          if (temporaryStorage_ != null) {
            temporaryStorage_.move(log);
            temporaryStorage_ = null;
          }
        }

        //
        // "data"
        //
        if (key.equals("data")) {
          // If we didn't read "curves" yet, create a temporary storage for data
          if (log.getCurves().isEmpty())
            temporaryStorage_ = new TemporaryStorage();

          readData(jsonParser, log,
                   shouldReadBulkData,
                   dataListener);
        }
      }
    }

    throw new IOException("Invalid JSON content: " + getSource());
  }

  /**
   * Read data for a set of JSON logs where the metadata has
   * already been read. This will preserve the existing JsonLog
   * structure in case JSON content is read in two operations:
   *
   * <pre>
   *   // Read meta data
   *   List&lt;JsonLog&gt; logs = reader.read(false, ...);
   *
   *   // Read the curve data
   *   reader.readData(logs);
   * </pre>
   *
   * There is nothing to gain in performance with this approach
   * so in case the result is not cached, the following will
   * be equivalent:
   *
   * <pre>
   *   // Read metadata
   *   List&lt;JsonLog&gt; logs = reader.read(false, ...);
   *
   *   // Read all the data
   *   logs = reader.read(true, ...);
   * </pre>
   *
   * @param logs          The logs to populate. These must be the
   *                      exact same list as retrieved by calling the
   *                      #read(false,...) on the same JsonReader instance.
   *                      Otherwise the behavior is unpredictable.
   * @param dataListener  Listener that will be notified when new data has been read.
   *                      Null if not used.
   * @throws IllegalArgumentException  If logs is null.
   * @throws IOException  If the read operation fails for some reason.
   * @throws InterruptedException  If the client returns <tt>false</tt> from
   *                      the {@link JsonDataListener#dataRead} method.
   */
  public void readData(List<JsonLog> logs, JsonDataListener dataListener)
    throws IOException, InterruptedException
  {
    if (logs == null)
      throw new IllegalArgumentException("logs cannot be null");

    // Read everything into a new structure
    List<JsonLog> newLogs = read(true, dataListener);

    // This is just a simple brain damage check. The client has all possible
    // ways to get into trouble if calling this method with an arbitrary argument.
    if (newLogs.size() != logs.size())
      throw new IllegalArgumentException("The specified logs are incompatible with the original");

    // Move the log data from the new to the existing
    for (int i = 0; i < logs.size(); i++) {
      JsonLog existingLog = logs.get(i);
      JsonLog newLog = newLogs.get(i);

      existingLog.setCurves(newLog.getCurves());
    }
  }

  /**
   * Read all logs from the content of this reader.
   *
   * @param shouldReadBulkData  True if bulk data should be read, false
   *                            if only metadata should be read.
   * @param dataListener        Client data listener. Null if not used.
   * @return                    The logs of the JSON stream. Never null.
   * @throws IOException        If the read operation fails for some reason.
   * @throws InterruptedException  If the client returns <tt>false</tt> from
   *                            the {@link JsonDataListener#dataRead} method.
   */
  public List<JsonLog> read(boolean shouldReadBulkData, JsonDataListener dataListener)
    throws IOException, InterruptedException
  {
    List<JsonLog> logs = new ArrayList<>();

    InputStream inputStream = null;

    try {
      if (file_ != null)
        inputStream = new FileInputStream(file_);
      else if (inputStream_ != null)
        inputStream = inputStream_;

      JsonParser jsonParser = jsonArray_ != null ? Json.createParserFactory(null).createParser(jsonArray_) : Json.createParser(inputStream);

      while (jsonParser.hasNext()) {
        JsonParser.Event parseEvent = jsonParser.next();

        if (parseEvent == JsonParser.Event.END_ARRAY)
          return logs;

        if (parseEvent == JsonParser.Event.START_OBJECT) {
          JsonLog log = readLog(jsonParser,
                                shouldReadBulkData,
                                dataListener);
          logs.add(log);
        }
      }

      jsonParser.close();

      return logs;
    }
    catch (IOException exception) {
      throw exception;
    }
    catch (InterruptedException exception) {
      throw exception;
    }
    catch (JsonException exception) {
      throw new IOException("Unable to read", exception);
    }
    finally {
      // We only close in the file input case.
      // Otherwise the client manage the stream.
      if (file_ != null && inputStream != null)
        inputStream.close();
    }
  }

  /**
   * Read all logs from the content of this reader.
   *
   * @return  The logs of the JSON stream. Never null.
   * @throws IOException  If the read operation fails for some reason.
   */
  public List<JsonLog> read()
    throws IOException
  {
    try {
      return read(true, null);
    }
    catch (InterruptedException exception) {
      assert false : "This will never happen: " + exception;
      return null;
    }
  }

  /**
   * Read all logs from the content of this reader and return
   * the first one. This is a convenience method if the client knows
   * that the source contains exactly one log.
   *
   * @return  The first log of the JSON stream. Null if there is none.
   * @throws IOException  If the read operation fails for some reason.
   */
  public JsonLog readOne()
    throws IOException
  {
    List<JsonLog> logs = read();
    return !logs.isEmpty() ? logs.get(0) : null;
  }


  /**
   * JSON does not guarantee the order of properties within an object so we
   * must be prepared for the case that "data" is stored before "curves" in
   * the input stream. Since we need the "curves" entrry to make properly
   * sense of the data, we store the data temporarily in this class and then
   * move them to the curves after these has been created.
   *
   * @author <a href="mailto:jacob.dreyer@geosoft.no">Jacob Dreyer</a>
   */
  private final class TemporaryStorage
  {
    /** Temporary storage: List of curves with list of dimensions of values. */
    private final List<List<List<Object>>> data_ = new ArrayList<>();

    /**
     * Create a temporary storage for JWLF data.
     */
    public TemporaryStorage()
    {
      // Nothing
    }

    /**
     * Add the specified value to the given curve/dimension.
     *
     * @param curveNo    Curve number to add to. [0,&gt;.
     * @param dimension  Dimension to add to. [0,&gt;.
     * @param value      Value to add. May be null for absent.
     */
    void add(int curveNo, int dimension, Object value)
    {
      if (curveNo >= data_.size())
        data_.add(new ArrayList<List<Object>>());

      List<List<Object>> curveData = data_.get(curveNo);

      if (dimension >= curveData.size())
        curveData.add(new ArrayList<Object>());

      List<Object> values = curveData.get(dimension);
      values.add(value);
    }

    /**
     * Move data from this temporary storage to the specified log.
     *
     * @param log  Log to move data to. Non-null.
     */
    void move(JsonLog log)
    {
      assert log != null : "log cannot be null";

      List<JsonCurve> curves = log.getCurves();

      // TODO: Remove data from the store as we go so we don't
      // end up sitting on twice the memory necessary

      for (int curveNo = 0; curveNo < data_.size(); curveNo++) {
        List<List<Object>> curveData = data_.get(curveNo);
        JsonCurve curve = curves.get(curveNo);
        for (int dimension = 0; dimension < curveData.size(); dimension++) {
          List<Object> values = curveData.get(dimension);
          for (int index = 0; index < values.size(); index++) {
            Object value = values.get(index);
            curve.addValue(dimension, value);
          }
        }
      }
    }
  }

  /**
   * Testing this class
   *
   * @param arguments  Application arguments. Not used.
   */
  private static void main(String[] arguments)
  {
    try {
      File file = new File("C:/Users/jacob/logdata/json/WLC_COMPOSITE_TINY.JSON");
      JsonReader reader = new JsonReader(file);
      List<JsonLog> logs = reader.read();

      System.out.println(JsonWriter.toString(logs.get(0)));
    }
    catch (Exception exception) {
      exception.printStackTrace();
    }
  }
}
