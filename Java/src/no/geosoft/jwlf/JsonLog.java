package no.geosoft.jwlf;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

/**
 * Class representing the content of one JSON Well Log Format log.
 * A log consists of a header, curve definitions, and curve data.
 *
 * @see <a href="https://jsonwelllogformat.org">https://jsonwelllogformat.org</a>
 *
 * @author <a href="mailto:jacob.dreyer@geosoft.no">Jacob Dreyer</a>
 */
public final class JsonLog
{
  /**
   * The log header data as a single JSON object.
   * <p>
   * We keep all the metadata in JSON form as the JsonLog class as such
   * does not take control of its entire content. Metadata may contain
   * anything the client like as long as it is valid JSON.
   */
  private JsonObject header_;

  /** The curves of this JSON log. */
  private final List<JsonCurve> curves_ = new CopyOnWriteArrayList<>();

  /** Indicate if this instance includes curve data or not. */
  private boolean hasCurveData_;

  /**
   * Create a new JSON log instance.
   *
   * @param hasCurveData  Indicate if the log includes curve data.
   */
  JsonLog(boolean hasCurveData)
  {
    hasCurveData_ = hasCurveData;
  }

  /**
   * Create an empty JSON Well Log Format log instance.
   */
  public JsonLog()
  {
    this(true); // It has all the curve data that exists (none)

    // Default empty header
    header_ = Json.createObjectBuilder().build();
  }

  /**
   * Create a JSON Well Log Format log instance as a copy of the
   * specified log.
   *
   * @param log                 Log to copy. Non-null.
   * @param includeCurveValues  True to include curve values in the copy, false if not.
   * @throws IllegalArgumentException  If log is null.
   */
  public JsonLog(JsonLog log, boolean includeCurveValues)
  {
    if (log == null)
      throw new IllegalArgumentException("log cannot be null");

    // Create empty header
    header_ = Json.createObjectBuilder().build();

    // Populate with values from log
    for (String key : log.getProperties())
      setProperty(key, log.getProperty(key));

    // Add a copy of the curves with or without curve values
    for (JsonCurve curve : log.getCurves())
      addCurve(new JsonCurve(curve, includeCurveValues));
  }

  /**
   * Return whether the JSON log instance includes curve data
   * or not, i.e.&nbsp;if only header data was read or created.
   *
   * @return  True if bulk (curve) data is present, false otherwise.
   */
  public boolean hasCurveData()
  {
    return hasCurveData_;
  }

  /**
   * Set the header of this instance.
   *
   * @param header  JSON header object. Non-null.
   * @throws IllegalArgumentEception  If header is null.
   */
  public void setHeader(JsonObject header)
  {
    if (header == null)
      throw new IllegalArgumentException("header cannot be null");

    synchronized (this) {
      // This is safe as JsonObject is immutable
      header_ = header;
    }
  }

  /**
   * Return the header of this log as a single JSON object.
   *
   * @return  Header of this log. Never null.
   */
  public JsonObject getHeader()
  {
    synchronized (this) {
      // This is safe as JsonObject is immutable
      return header_;
    }
  }

  /**
   * Set a string header property of this log.
   *
   * @param key    Key of property to set. Non-null.
   * @param value  Associated value. Null to unset. Must be of type
   *               BigDecimal, BigInteger, Boolean, Double, Integer,
   *               Long, String, Date or JsonValue.
   * @throws IllegalArgumentException  If key is null or value is not of a
   *               legal primitive type.
   */
  public void setProperty(String key, Object value)
  {
    if (key == null)
      throw new IllegalArgumentException("key cannot be null");

    if (value != null &&
        !(value instanceof BigDecimal) &&
        !(value instanceof BigInteger) &&
        !(value instanceof Boolean) &&
        !(value instanceof Double) &&
        !(value instanceof Integer) &&
        !(value instanceof Long) &&
        !(value instanceof String) &&
        !(value instanceof Date) &&
        !(value instanceof JsonValue))
      throw new IllegalArgumentException("Invalid property type: " + value.getClass());

    JsonObjectBuilder objectBuilder = Json.createObjectBuilder();

    synchronized (this) {
      header_.forEach(objectBuilder::add);
      JsonUtil.add(objectBuilder, key, value);
      setHeader(objectBuilder.build());
    }
  }

  /**
   * Return all the header property keys of this log.
   *
   * @return  All property keys of this log. Never null.
   */
  public Set<String> getProperties()
  {
    return getHeader().keySet();
  }

  /**
   * Return header property for the specified key.
   * <p>
   * This is a generic method for clients that knows custom content
   * of the well log. It is up to the client program to parse the returned
   * content into the appropriate type.
   *
   * @param key  Key of property to get. Non-null.
   * @return     The associated value, or null if not found.
   * @throws IllegalArgumentException  If key is null.
   */
  public Object getProperty(String key)
  {
    if (key == null)
      throw new IllegalArgumentException("key cannot be null");

    JsonValue value = getHeader().get(key);
    return value != null ? JsonUtil.getValue(value) : null;
  }

  /**
   * Return header property for the specified key as a string.
   *
   * @param key  Key of property to get. Non-null.
   * @return     The associated value as a string. Null if not found, or
   *             not compatible with the string type.
   * @throws IllegalArgumentException  If key is null.
   */
  public String getPropertyAsString(String key)
  {
    if (key == null)
      throw new IllegalArgumentException("key cannot be null");

    Object object = getProperty(key);

    // Since Util.getAsType() return null for empty string
    if (object instanceof String && object.toString().isEmpty())
      return "";

    return (String) Util.getAsType(object, String.class);
  }

  /**
   * Return header property for the specified key as a double.
   *
   * @param key  Key of property to get. Non-null.
   * @return     The associated value as a double. Null if not found, or
   *             not compatible with the double type.
   * @throws IllegalArgumentException  If key is null.
   */
  public Double getPropertyAsDouble(String key)
  {
    if (key == null)
      throw new IllegalArgumentException("key cannot be null");

    return (Double) Util.getAsType(getProperty(key), Double.class);
  }

  /**
   * Return header property for the specified key as an integer.
   *
   * @param key  Key of property to get. Non-null.
   * @return     The associated value as an integer. Null if not found, or
   *             not compatible with the integer type.
   * @throws IllegalArgumentException  If key is null.
   */
  public Integer getPropertyAsInteger(String key)
  {
    if (key == null)
      throw new IllegalArgumentException("key cannot be null");

    return (Integer) Util.getAsType(getProperty(key), Integer.class);
  }

  /**
   * Return header property for the specified key as a boolean.
   *
   * @param key  Key of property to get. Non-null.
   * @return     The associated value as a boolean. Null if not found, or
   *             not compatible with the boolean type.
   * @throws IllegalArgumentException  If key is null.
   */
  public Boolean getPropertyAsBoolean(String key)
  {
    if (key == null)
      throw new IllegalArgumentException("key cannot be null");

    return (Boolean) Util.getAsType(getProperty(key), Boolean.class);
  }

  /**
   * Return header property for the specified key as date.
   *
   * @param key  Key of property to get. Non-null.
   * @return     The associated value as a date. Null if not found, or
   *             not compatible with the date type.
   * @throws IllegalArgumentException  If key is null.
   */
  public Date getPropertyAsDate(String key)
  {
    if (key == null)
      throw new IllegalArgumentException("key cannot be null");

    return (Date) Util.getAsType(getProperty(key), Date.class);
  }

  /**
   * Return all property <em>tables</em> from the header.
   *
   * @return  All property tables from the header. Never null.
   */
  public List<JsonTable> getTables()
  {
    List<JsonTable> tables = new ArrayList<>();

    synchronized (this) {
      for (Map.Entry<String,JsonValue> entry : header_.entrySet()) {
        String key = entry.getKey();
        JsonValue value = entry.getValue();

        if (value instanceof JsonObject && JsonTable.isTable((JsonObject) value))
          tables.add(JsonTable.create(key, (JsonObject) value));
      }
    }

    return tables;
  }

  /**
   * Return property table of the specified name.
   *
   * @param tableName  Name of property table to get.
   * @return           The requested table, or null if not present.
   * @throws IllegalArgumentException  If tableName is null.
   */
  public JsonTable getTable(String tableName)
  {
    if (tableName == null)
      throw new IllegalArgumentException("tableName cannot be null");

    for (JsonTable table : getTables()) {
      if (tableName.equals(table.getName()))
        return table;
    }

    // Not found
    return null;
  }

  /**
   * Add the specified property table to this log.
   *
   * @param table  Table to add. Non-null.
   * @throws IllegalArgumentException  If table is null or a table with the
   *               same name already exists.
   */
  public void addTable(JsonTable table)
  {
    if (table == null)
      throw new IllegalArgumentException("table cannot be null");

    if (getTable(table.getName()) != null)
      throw new IllegalArgumentException("table already exists: " + table.getName());

    JsonObject tableObject = table.asJsonObject();

    JsonObjectBuilder objectBuilder = Json.createObjectBuilder();

    synchronized (this) {
      header_.forEach(objectBuilder::add);
      objectBuilder.add(table.getName(), tableObject);
      setHeader(objectBuilder.build());
    }
  }

  /**
   * Return name of this log.
   *
   * @return  Name of this log. Null if none provided.
   */
  public String getName()
  {
    return getPropertyAsString(JsonWellLogProperty.NAME.getKey());
  }

  /**
   * Set name of this log.
   *
   * @param name  Name to set. Null to unset.
   */
  public void setName(String name)
  {
    setProperty(JsonWellLogProperty.NAME.getKey(), name);
  }

  /**
   * Get description of this log.
   *
   * @return  Description of this log. Null if none provided.
   */
  public String getDescription()
  {
    return getPropertyAsString(JsonWellLogProperty.DESCRIPTION.getKey());
  }

  /**
   * Set description of this log.
   *
   * @param description  Description to set. Null to unset.
   */
  public void setDescription(String description)
  {
    setProperty(JsonWellLogProperty.DESCRIPTION.getKey(), description);
  }

  /**
   * Return well name of this log.
   *
   * @return  Well name of this log. Null if none provided.
   */
  public String getWell()
  {
    return getPropertyAsString(JsonWellLogProperty.WELL.getKey());
  }

  /**
   * Set well name of this log.
   *
   * @param well  Well name to set. Null to unset.
   */
  public void setWell(String well)
  {
    setProperty(JsonWellLogProperty.WELL.getKey(), well);
  }

  /**
   * Return wellbore name of this log.
   *
   * @return  Wellbore name of this log. Null if none provided.
   */
  public String getWellbore()
  {
    return getPropertyAsString(JsonWellLogProperty.WELLBORE.getKey());
  }

  /**
   * Set wellbore name of this log.
   *
   * @param wellbore  Wellbore name to set. Null to unset.
   */
  public void setWellbore(String wellbore)
  {
    setProperty(JsonWellLogProperty.WELLBORE.getKey(), wellbore);
  }

  /**
   * Return field name of this log.
   *
   * @return  Field name of this log. Null if none provided.
   */
  public String getField()
  {
    return getPropertyAsString(JsonWellLogProperty.FIELD.getKey());
  }

  /**
   * Set field name of this log.
   *
   * @param field  Field name to set. Null to unset.
   */
  public void setField(String field)
  {
    setProperty(JsonWellLogProperty.FIELD.getKey(), field);
  }

  /**
   * Return country of this log.
   *
   * @return  Country of this log. Null if none provided.
   */
  public String getCountry()
  {
    return getPropertyAsString(JsonWellLogProperty.COUNTRY.getKey());
  }

  /**
   * Set country of this log.
   *
   * @param country  Country to set. Null to unset.
   */
  public void setCountry(String country)
  {
    setProperty(JsonWellLogProperty.COUNTRY.getKey(), country);
  }

  /**
   * Return logging date of this log.
   *
   * @return  Logging date of this log. Null if none provided.
   */
  public Date getDate()
  {
    return getPropertyAsDate(JsonWellLogProperty.DATE.getKey());
  }

  /**
   * Set logging date of this log.
   *
   * @param date  Logging date to set. Null to unset.
   */
  public void setDate(Date date)
  {
    setProperty(JsonWellLogProperty.DATE.getKey(), date);
  }

  /**
   * Return operator name of this log.
   *
   * @return  Operator name of this log. Null if none provided.
   */
  public String getOperator()
  {
    return getPropertyAsString(JsonWellLogProperty.OPERATOR.getKey());
  }

  /**
   * Set operator name of this log.
   *
   * @param operator  Operator name to set. Null to unset.
   */
  public void setOperator(String operator)
  {
    setProperty(JsonWellLogProperty.OPERATOR.getKey(), operator);
  }

  /**
   * Return service company name of this log.
   *
   * @return  Service company name of this log. Null if none provided.
   */
  public String getServiceCompany()
  {
    return getPropertyAsString(JsonWellLogProperty.SERVICE_COMPANY.getKey());
  }

  /**
   * Set service company name of this log.
   *
   * @param serviceCompany  Service company name of this log. Null to unset.
   */
  public void setServiceCompany(String serviceCompany)
  {
    setProperty(JsonWellLogProperty.SERVICE_COMPANY.getKey(), serviceCompany);
  }

  /**
   * Return run number of this log.
   *
   * @return  Run number of this log. Null if none provided.
   */
  public String getRunNumber()
  {
    return getPropertyAsString(JsonWellLogProperty.RUN_NUMBER.getKey());
  }

  /**
   * Set run number of this log.
   *
   * @param runNumber  Run number of this log. Null to unset.
   */
  public void setRunNumber(String runNumber)
  {
    setProperty(JsonWellLogProperty.RUN_NUMBER.getKey(), runNumber);
  }

  /**
   * Return elevation of this log.
   *
   * @return  Elevation of this log. Null if none provided.
   */
  public String getElevation()
  {
    return getPropertyAsString(JsonWellLogProperty.ELEVATION.getKey());
  }

  /**
   * Set elevation of this log.
   *
   * @param elevation  Elevation of this log. Null to unset.
   */
  public void setElevation(double elevation)
  {
    setProperty(JsonWellLogProperty.ELEVATION.getKey(), elevation);
  }

  /**
   * Return the source (system or process) of this log.
   *
   * @return  Source of this log. Null if none provided.
   */
  public String getSource()
  {
    return getPropertyAsString(JsonWellLogProperty.SOURCE.getKey());
  }

  /**
   * Set source (system or process) of this log.
   *
   * @param source  Source of this log. Null to unset.
   */
  public void setSource(String source)
  {
    setProperty(JsonWellLogProperty.SOURCE.getKey(), source);
  }

  /**
   * Return URI location of the data object in case this is kept separate.
   *
   * @return  URI location of the data object. Null if data is kept locale.
   */
  public String getDataUri()
  {
    return getPropertyAsString(JsonWellLogProperty.DATA_URI.getKey());
  }

  /**
   * Set URI for the data object in case this is kept separate.
   *
   * @param dataUri  URI to the data object. Null if data is kept local.
   */
  public void setDataUri(String dataUri)
  {
    setProperty(JsonWellLogProperty.DATA_URI.getKey(), dataUri);
  }

  /**
   * Return value type of the index of this log, typically Double.class
   * or Date.class.
   *
   * @return Value type of the index of this log. Never null.
   *         If the log has no curves, Double.class is returned.
   */
  public Class<?> getIndexValueType()
  {
    return curves_.isEmpty() ? Double.class : curves_.get(0).getValueType();
  }

  /**
   * Return start index of this log.
   * <p>
   * <b>NOTE: </b> This property is taken from the header, and may not
   * necessarily be in accordance with the <em>actual</em> data of the log.
   *
   * @return Start index of this log. The type will be according to
   *         the type of the index curve, @see #getIndexValueType.
   */
  public Object getStartIndex()
  {
    return getIndexValueType() == Date.class ?
      getPropertyAsDate(JsonWellLogProperty.START_INDEX.getKey()) :
      getPropertyAsDouble(JsonWellLogProperty.START_INDEX.getKey());
  }

  /**
   * Return the <em>actual</em> start index of this log.
   *
   * @return  The actual start index of this log. Null if the log has no values.
   */
  public Object getActualStartIndex()
  {
    JsonCurve indexCurve = !curves_.isEmpty() ? curves_.get(0) : null;
    int nValues = indexCurve != null ? indexCurve.getNValues() : 0;
    return nValues > 0 ? indexCurve.getValue(0) : null;
  }

  /**
   * Set start index of this log in header.
   *
   * @param startIndex  Start index to set. Null to unset. The type should
   *                    be in accordance with the actual type of the index curve
   *                    of the log.
   */
  public void setStartIndex(Object startIndex)
  {
    if (startIndex instanceof Date)
      setProperty(JsonWellLogProperty.START_INDEX.getKey(), startIndex);
    else
      setProperty(JsonWellLogProperty.START_INDEX.getKey(), Util.getAsDouble(startIndex));
  }

  /**
   * Return end index of this log.
   * <p>
   * <b>NOTE: </b> This property is taken from header, and may not
   * necessarily be in accordance with the <em>actual</em> data of the log.
   *
   * @return End index of this log. The type will be according to
   *         the type of the index curve, @see #getIndexValueType.
   */
  public Object getEndIndex()
  {
    return getIndexValueType() == Date.class ?
      getPropertyAsDate(JsonWellLogProperty.END_INDEX.getKey()) :
      getPropertyAsDouble(JsonWellLogProperty.END_INDEX.getKey());
  }

  /**
   * Return the <em>actual</em> end index of this log.
   *
   * @return  The actual end index of this log. Null if the log has no values.
   */
  public Object getActualEndIndex()
  {
    JsonCurve indexCurve = !curves_.isEmpty() ? curves_.get(0) : null;
    int nValues = indexCurve != null ? indexCurve.getNValues() : 0;
    return nValues > 0 ? indexCurve.getValue(nValues - 1) : null;
  }

  /**
   * Set end index of this log in the header.
   *
   * @param endIndex  End index to set. Null to unset. The type should
   *                  be in accordance with the actual type of the index curve
   *                  of the log.
   */
  public void setEndIndex(Object endIndex)
  {
    if (endIndex instanceof Date)
      setProperty(JsonWellLogProperty.END_INDEX.getKey(), endIndex);
    else
      setProperty(JsonWellLogProperty.END_INDEX.getKey(), Util.getAsDouble(endIndex));
  }

  /**
   * Return the regular step of this log.
   * <p>
   * <b>NOTE: </b> This property is taken from header, and may not
   * necessarily be in accordance with the <em>actual</em> data on the file.
   *
   * @return The step of the index curve of this log.
   *         Null should indicate that the log in irregular or the step is unknown.
   */
  public Double getStep()
  {
    return getPropertyAsDouble(JsonWellLogProperty.STEP.getKey());
  }

  /**
   * Return the <em>actual</em> step of the index curve of this log.
   *
   * @return  The actual step of the index curve.
   *          Null if the log has no data or the log set is irregular.
   */
  public Double getActualStep()
  {
    return JsonUtil.computeStep(this);
  }

  /**
   * Set the regular step of the index curve of this log.
   *
   * @param step  Step to set. Null to indicate unknown or that the index is irregular.
   *              If the log set is time based, the step should be the number
   *              of <em>milliseconds</em> between samples.
   */
  public void setStep(Double step)
  {
    setProperty(JsonWellLogProperty.STEP.getKey(), step);
  }

  /**
   * Add the specified curve to this log. The first curve added to a log
   * is by convention the index curve.
   *
   * @param curve  Curve to add. Non-null.
   * @throws IllegalArgumentException  If curve is null.
   */
  public void addCurve(JsonCurve curve)
  {
    if (curve == null)
      throw new IllegalArgumentException("curve cannot be null");

    curves_.add(curve);
  }

  /**
   * Return the curves of this log. The first curve
   * is by convention always the index curve.
   *
   * @return  The curves of this log. Never null.
   */
  public List<JsonCurve> getCurves()
  {
    return Collections.unmodifiableList(curves_);
  }

  /**
   * Return the specified curve from this log.
   *
   * @param curveNo  Curve number of curve to return. [0,nCurves&gt;.
   * @return         The requested curve. Never null.
   * @throws IllegalArgumentException  If curveNo is out of bound.
   */
  public JsonCurve getCurve(int curveNo)
  {
    if (curveNo < 0 || curveNo >= curves_.size())
      throw new IllegalArgumentException("Invalid curveNo: " + curveNo);

    return curves_.get(curveNo);
  }

  /**
   * Return curve of the specified name from this log.
   * <p>
   * If there happens to be more than one curve with this name
   * the first one encountered is returned.
   *
   * @param curveName  Name of curve to find.
   * @return           The requested curve, or null if not found.
   * @throws IllegalArgumentException  If curveName is null.
   */
  public JsonCurve findCurve(String curveName)
  {
    if (curveName == null)
      throw new IllegalArgumentException("curveName cannot be null");

    for (JsonCurve curve : curves_)
      if (curve.getName().equals(curveName))
        return curve;

    // Not found
    return null;
  }

  /**
   * Return curve number of the curve with the specified name.
   * <p>
   * If there happens to be more than one curve with this name
   * the first one encountered is returned.
   *
   * @param curveName  Name of curve to find.
   * @return           The requested curve number, or -1 if not found.
   * @throws IllegalArgumentException  If curveName is null.
   */
  public int findCurveNo(String curveName)
  {
    if (curveName == null)
      throw new IllegalArgumentException("curveName cannot be null");

    for (int curveNo = 0; curveNo < curves_.size(); curveNo++) {
      JsonCurve curve = curves_.get(curveNo);
      if (curve.getName().equals(curveName))
        return curveNo;
    }

    // Not found
    return -1;
  }

  /**
   * Replace the present set of curves.
   * <p>
   * This method is called by the reader to populate a JsonLog instance
   * that initially was read without bulk data.
   *
   * @param curves  Curves to set. Non-null.
   */
  void setCurves(List<JsonCurve> curves)
  {
    assert curves != null : "curves cannot be null";

    // TODO: Not thread safe. Need an atomic replacement for these two
    curves_.clear();
    curves_.addAll(curves);

    hasCurveData_ = true;
  }

  /**
   * Return the number of curves in this log.
   *
   * @return  Number of curves in this log. [0,&gt;.
   */
  public int getNCurves()
  {
    return curves_.size();
  }

  /**
   * Return the number of values (per curve) in this log.
   *
   * @return  Number of values in this log. [0,&gt;.
   */
  public int getNValues()
  {
    return curves_.isEmpty() ? 0 : curves_.get(0).getNValues();
  }

  /**
   * Return the index curve of this log.
   *
   * @return  The index curve of this log, or null if the
   *          log doesn't contain any curves.
   */
  public JsonCurve getIndexCurve()
  {
    return getNCurves() > 0 ? getCurves().get(0) : null;
  }

  /**
   * Clear curve data from all curves of this log.
   */
  public void clearCurves()
  {
    for (JsonCurve curve : curves_)
      curve.clear();
  }

  /**
   * Set curve capacity to actual size to save memory.
   * The assumption is that the curves will not grow any further.
   */
  void trimCurves()
  {
    for (JsonCurve curve : curves_)
      curve.trim();
  }

  /**
   * Return number of significant digits to use to properly represent
   * the values of the specified curve.
   *
   * @param curve         Curve to consider. Non-null.
   * @param isIndexCurve  True if curve is an index curve, false otherwise.
   * @return              The number of significant digits to use for the
   *                      specified curve. [0,&gt;.
   */
  private int getNSignificantDigits(JsonCurve curve, boolean isIndexCurve)
  {
    assert curve != null : "curve cannot be null";

    Class<?> valueType = curve.getValueType();

    // Limit to platform capabilities (see Util.getNSignificantDigits)
    int maxSignificantDigits = 10;

    if (valueType != Double.class && valueType != Float.class)
      return 0;

    if (curve.getNValues() == 0)
      return 0;

    if (!isIndexCurve)
      return maxSignificantDigits;

    //
    // Special treatment for the index curve so we don't accidently
    // lose accuracy; making a regular log set irregular.
    //

    Object[] range = curve.getRange();
    if (range[0] == null || range[1] == null)
      return maxSignificantDigits;

    Double step = JsonUtil.computeStep(this);
    if (step == null || step == 0.0)
      return maxSignificantDigits;

    double minValue = Util.getAsDouble(range[0]);
    double maxValue = Util.getAsDouble(range[1]);

    double max = Math.max(Math.abs(minValue), Math.abs(maxValue));

    return Util.getNSignificantDigits(max, step);
  }

  /**
   * Create a formatter for the data of the specified curve.
   *
   * @param curve         Curve to create formatter for. Non-null.
   * @param isIndexCurve  True if curve is the index curve, false otherwise.
   * @return  A formatter that can be used to write the curve data.
   *                      Null if the log data is not of numeric type.
   */
  Formatter createFormatter(JsonCurve curve, boolean isIndexCurve)
  {
    assert curve != null : "curve cannot be null";

    Class<?> valueType = curve.getValueType();
    if (valueType != Double.class && valueType != Float.class)
      return null;

    int nDimensions = curve.getNDimensions();
    int nValues = curve.getNValues();

    double[] values = new double[nValues * nDimensions];

    for (int index = 0; index < nValues; index++)
      for (int dimension = 0; dimension < nDimensions; dimension++)
        values[dimension * nValues + index] = Util.getAsDouble(curve.getValue(dimension, index));

    int nSignificantDigits = getNSignificantDigits(curve, isIndexCurve);

    return new Formatter(values, nSignificantDigits, null, null);
  }

  /** {@inheritDoc} */
  @Override
  public String toString()
  {
    StringBuilder s = new StringBuilder();
    s.append("-- JSON log\n");

    s.append("Header:\n");
    for (String property : getProperties())
      s.append(property + ": " + getProperty(property));

    for (JsonCurve curve : curves_)
      s.append(curve + "\n");

    return s.toString();
  }
}
