package no.geosoft.jwlf;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.stream.JsonParser;

/**
 * A collection of utilities for the Log I/O JSON module.
 *
 * @author <a href="mailto:jacob.dreyer@geosoft.no">Jacob Dreyer</a>
 */
final class JsonUtil
{
  /**
   * Private constructor to prevent client instantiation.
   */
  private JsonUtil()
  {
    assert false : "This constructor should never be called";
  }

  /**
   * Encode the given string by appropriate escape sequences
   * so that it can be used as a JSON literal.
   * <p>
   * <b>NOTE: </b>The returned value includes surrounding quotes.
   *
   * @param text  Text to encode. Non-null. Without quotes.
   * @return      Encoded text. Never null. Includes surrounding quotes.
   */
  static String encode(String text)
  {
    assert text != null : "text cannot be null";

    // We could scan the text and escape as necessary.
    // Better is to let javax.json fix this, but as there is no public
    // method that does this conversion we add the text to a phony JSON
    // object and then extract it again. Probably not as efficient, but
    // JSON Well Log Format doesn't include many strings anyway.
    return Json.createObjectBuilder().add("x", text).build().get("x").toString();
  }

  /**
   * Add the specified key/value to the given JSON object builder.
   *
   * @param jsonObjectBuilder  JSON object builder to add to. Non-null.
   * @param key                Key to add. Non-null.
   * @param value              Value of key. May be null.
   */
  static void add(JsonObjectBuilder jsonObjectBuilder, String key, Object value)
  {
    assert jsonObjectBuilder != null : "jsonObjectBuilder annot be null";
    assert key != null : "key cannot be null";

    if (value == null)
      jsonObjectBuilder.addNull(key);

    else if (value instanceof BigDecimal)
      jsonObjectBuilder.add(key, (BigDecimal) value);

    else if (value instanceof BigInteger)
      jsonObjectBuilder.add(key, (BigInteger) value);

    else if (value instanceof Boolean)
      jsonObjectBuilder.add(key, (Boolean) value);

    else if (value instanceof Double) {
      Double v = (Double) value;
      if (Double.isFinite(v))
        jsonObjectBuilder.add(key, v);
      else
        jsonObjectBuilder.addNull(key);
    }

    else if (value instanceof Float) {
      Float v = (Float) value;
      if (Float.isFinite(v))
        jsonObjectBuilder.add(key, v.doubleValue());
      else
        jsonObjectBuilder.addNull(key);
    }

    else if (value instanceof Integer)
      jsonObjectBuilder.add(key, (Integer) value);

    else if (value instanceof Long)
      jsonObjectBuilder.add(key, (Long) value);

    else if (value instanceof Short)
      jsonObjectBuilder.add(key, ((Number) value).intValue());

    else if (value instanceof Byte)
      jsonObjectBuilder.add(key, ((Number) value).intValue());

    else if (value instanceof String)
      jsonObjectBuilder.add(key, (String) value);

    else if (value instanceof Date)
      jsonObjectBuilder.add(key, ISO8601DateParser.toString((Date) value));

    else if (value instanceof JsonArray)
      jsonObjectBuilder.add(key, (JsonArray) value);

    else if (value instanceof JsonObject)
      jsonObjectBuilder.add(key, (JsonObject) value);

    else if (value instanceof JsonValue)
      add(jsonObjectBuilder, key, getValue((JsonValue) value));

    else
      assert false : "Unrecognized value type: " + value.getClass();
  }

  /**
   * Add the specified value to the given JSON array builder.
   *
   * @param jsonArrayBuilder  JSON array builder to add to. Non-null.
   * @param value             Value to add. May be null.
   */
  static void add(JsonArrayBuilder jsonArrayBuilder, Object value)
  {
    assert jsonArrayBuilder != null : "jsonArrayBuilder annot be null";

    if (value == null)
      jsonArrayBuilder.addNull();

    else if (value instanceof BigDecimal)
      jsonArrayBuilder.add((BigDecimal) value);

    else if (value instanceof BigInteger)
      jsonArrayBuilder.add((BigInteger) value);

    else if (value instanceof Boolean)
      jsonArrayBuilder.add((Boolean) value);

    else if (value instanceof Double) {
      Double v = (Double) value;
      if (Double.isFinite(v))
        jsonArrayBuilder.add(v);
      else
        jsonArrayBuilder.addNull();
    }

    else if (value instanceof Float) {
      Float v = (Float) value;
      if (Float.isFinite(v))
        jsonArrayBuilder.add(v.doubleValue());
      else
        jsonArrayBuilder.addNull();
    }

    else if (value instanceof Integer)
      jsonArrayBuilder.add((Integer) value);

    else if (value instanceof Long)
      jsonArrayBuilder.add((Long) value);

    else if (value instanceof Short)
      jsonArrayBuilder.add(((Number) value).intValue());

    else if (value instanceof Byte)
      jsonArrayBuilder.add(((Number) value).intValue());

    else if (value instanceof String)
      jsonArrayBuilder.add((String) value);

    else if (value instanceof Date)
      jsonArrayBuilder.add(ISO8601DateParser.toString((Date) value));

    else if (value instanceof JsonArray)
      jsonArrayBuilder.add((JsonArray) value);

    else if (value instanceof JsonObject)
      jsonArrayBuilder.add((JsonObject) value);

    else if (value instanceof JsonValue)
      add(jsonArrayBuilder, getValue((JsonValue) value));

    else
      assert false : "Unrecognized value type: " + value.getClass();
  }

  /**
   * Return the (first) key of the specified JSON object.
   * <p>
   * This is a convenience method if the client knows that the
   * object contains exactly one key.
   *
   * @param jsonObject  JSON object to get key from. Non-null.
   * @return            The requested key. Never null.
   */
  static String getKey(JsonObject jsonObject)
  {
    assert jsonObject != null : "jsonObject cannot be null";
    return jsonObject.keySet().iterator().next();
  }

  /**
   * Return the fundamental value of the specified JSON value.
   *
   * @param jsonValue  JSON value to get fundamental value from. Non-null.
   * @return           Requested value. May be null, if jsonValue is of NULL type.
   */
  static Object getValue(JsonValue jsonValue)
  {
    assert jsonValue != null : "jsonValue cannot be null";

    switch (jsonValue.getValueType()) {
      case ARRAY :
      case OBJECT :
        return jsonValue;

      case NUMBER :
        JsonNumber number = (JsonNumber) jsonValue;
        if (number.isIntegral())
          return number.intValueExact(); // TODO: Handle longs
        else
          return number.doubleValue();

      case STRING :
        return ((JsonString) jsonValue).getString();

      case TRUE :
        return true;

      case FALSE :
        return false;

      case NULL :
        return null;

      default:
        assert false : "Unrecognized value type: " + jsonValue.getValueType();
        return null;
    }
  }

  /**
   * Check if the specified JSON value represents a
   * primitive (number, string, boolean, null) type.
   *
   * @param jsonValue  JSON value to check. Non-null.
   * @return           True if the value represents a primitive type,
   *                   false if it represents a JSON object or a JSON array.
   */
  static boolean isPrimitive(JsonValue jsonValue)
  {
    assert jsonValue != null : "jsonValue cannot be null";
    return jsonValue.getValueType() != JsonValue.ValueType.ARRAY &&
           jsonValue.getValueType() != JsonValue.ValueType.OBJECT;
  }

  /**
   * Check if the specified JSON array contains any JSON objects.
   *
   * @param jsonArray  JSON array to check. Non-null.
   * @return           True if the array contains any JSON objects,
   *                   false otherwise.
   */
  static boolean containsObjects(JsonArray jsonArray)
  {
    for (JsonValue value : jsonArray) {
      if (value instanceof JsonArray && containsObjects((JsonArray) value) || value instanceof JsonObject)
        return true;
    }

    // The array (or sub arrays) contains no objects
    return false;
  }

  /**
   * Read a JSON array from the current location of the JSON parser.
   *
   * @param jsonParser  The JSON parser. Non-null.
   * @return            The JSON array builder. Never null.
   */
  static JsonArrayBuilder readJsonArray(JsonParser jsonParser)
  {
    assert jsonParser != null : "jsonParser cannot be null";

    JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

    while (jsonParser.hasNext()) {
      JsonParser.Event parseEvent = jsonParser.next();

      switch (parseEvent) {
        case START_OBJECT :
          JsonObjectBuilder objectBuilder = readJsonObject(jsonParser);
          arrayBuilder.add(objectBuilder);
          break;

        case END_OBJECT :
          assert false : "Invalid state";
          break;

        case START_ARRAY :
          JsonArrayBuilder subArrayBuilder = readJsonArray(jsonParser);
          arrayBuilder.add(subArrayBuilder);
          break;

        case END_ARRAY :
          return arrayBuilder;

        case VALUE_FALSE :
          arrayBuilder.add(false);
          break;

        case VALUE_TRUE :
          arrayBuilder.add(true);
          break;

        case VALUE_NULL :
          arrayBuilder.addNull();
          break;

        case VALUE_NUMBER :
          arrayBuilder.add(jsonParser.getBigDecimal());
          break;

        case VALUE_STRING :
          arrayBuilder.add(jsonParser.getString());
          break;

        default :
          assert false : "Unrecognized event: " + parseEvent;
      }
    }

    assert false : "Invalid state";
    return null;
  }

  /**
   * Read a JSON object from the current location of the JSON parser.
   *
   * @param jsonParser  The JSON parser. Non-null.
   * @return            The JSON object builder. Never null.
   */
  static JsonObjectBuilder readJsonObject(JsonParser jsonParser)
  {
    assert jsonParser != null : "jsonParser cannot be null";

    JsonObjectBuilder objectBuilder = Json.createObjectBuilder();

    String key = null;

    while (jsonParser.hasNext()) {
      JsonParser.Event parseEvent = jsonParser.next();

      switch (parseEvent) {
        case KEY_NAME :
          key = jsonParser.getString();
          break;

        case START_OBJECT :
          if (key != null) {
            JsonObjectBuilder subObjectBuilder = readJsonObject(jsonParser);
            objectBuilder.add(key, subObjectBuilder);
          }
          break;

        case END_OBJECT :
          return objectBuilder;

        case START_ARRAY :
          JsonArrayBuilder arrayBuilder = readJsonArray(jsonParser);
          objectBuilder.add(key, arrayBuilder);
          break;

        case END_ARRAY :
          assert false : "Invalid state";
          break;

        case VALUE_FALSE :
          objectBuilder.add(key, false);
          break;

        case VALUE_TRUE :
          objectBuilder.add(key, true);
          break;

        case VALUE_NULL :
          objectBuilder.addNull(key);
          break;

        case VALUE_NUMBER :
          objectBuilder.add(key, jsonParser.getBigDecimal());
          break;

        case VALUE_STRING :
          objectBuilder.add(key, jsonParser.getString());
          break;

        default :
          assert false : "Unrecognized event: " + parseEvent;
      }
    }

    assert false : "Invalid state";
    return null;
  }

  /**
   * Find actual step value of the specified JSON log, being the distance between
   * values in the index curve. Three values are returned: the <em>minimum step</em>,
   * the <em>maximum step</em> and the <em>average step</em>. It is left to the client
   * to decide if these numbers represents a <em>regular</em> or an <em>irregular</em>
   * log set.
   *
   * @param log  Log to get step from. Non-null.
   * @return     The (minimum, maximum and average) step value of the log.
   */
  private static double[] findStep(JsonLog log)
  {
    assert log != null : "log cannot be null";

    List<JsonCurve> curves = log.getCurves();

    JsonCurve indexCurve = !curves.isEmpty() ? curves.get(0) : null;
    int nValues = indexCurve != null ? indexCurve.getNValues() : 0;

    if (nValues < 2)
      return new double[] {0.0, 0.0, 0.0};

    double minStep = +Double.MAX_VALUE;
    double maxStep = -Double.MAX_VALUE;
    double averageStep = 0.0;

    int nSteps = 0;
    double indexValue0 = Util.getAsDouble(indexCurve.getValue(0));
    for (int index = 1; index < nValues; index++) {
      double indexValue1 = Util.getAsDouble(indexCurve.getValue(index));
      double step = indexValue1 - indexValue0;

      nSteps++;

      if (step < minStep)
        minStep = step;

      if (step > maxStep)
        maxStep = step;

      averageStep += (step - averageStep) / nSteps;

      indexValue0 = indexValue1;
    }

    return new double[] {minStep, maxStep, averageStep};
  }

  /**
   * Based on the index curve, compute the step value of the specified log
   * as it will be reported in the <em>step</em> metadata.
   * <p>
   * The method uses the {@link JsonUtil#findStep} method to compute min, max and
   * average step, and then compare the largest deviation from the average
   * (min or max) to the average itself.
   * If this is within some limit (0.5% currently) the step is considered
   * regular.
   *
   * @param log  Log to compute step of. Non-null.
   * @return     The log step value. null if irregular.
   */
  static Double computeStep(JsonLog log)
  {
    assert log != null : "log cannot be null";

    double[] step = findStep(log);

    double minStep = step[0];
    double maxStep = step[1];
    double averageStep = step[2];

    // Find largest deviation from average of the two
    double d = Math.max(Math.abs(minStep - averageStep), Math.abs(maxStep - averageStep));

    // Figure out if this is close enough to regard as equal
    // NOTE: If this number causes apparently regular log sets to appear irregular
    // we might consider adjusting it further, probably as high as 0.01 would be OK.
    boolean isEqual = d <= Math.abs(averageStep) * 0.005;

    return isEqual ? averageStep : null;
  }
}
