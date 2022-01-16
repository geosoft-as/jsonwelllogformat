package no.geosoft.jwlf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.json.JsonString;

/**
 * Class for modeling a JSON <em>table</em>. Useful for holding metadata
 * originating from old well log formats like DLIS, LIS, LAS etc.
 * <p>
 * The JSON table has the following structure:
 * <pre>
 *   "&lt;name&gt;" : {
 *     "<b>attributes</b>": ["&lt;attr1&gt;", "&lt;attr2&gt;", "&lt;attr3&gt;", ... "&lt;attrn&gt;"],
 *     "<b>objects</b>": [
 *       "&lt;object1&gt;": [&lt;v11&gt;, &lt;v12&gt;, &lt;v13&gt;, ... &lt;v1n&gt;],
 *       "&lt;object2&gt;": [&lt;v21&gt;, &lt;v22&gt;, &lt;v23&gt;, ... &lt;v2n&gt;],
 *       "&lt;object3&gt;": [&lt;v31&gt;, &lt;v32&gt;, &lt;v33&gt;, ... &lt;v3n&gt;],
 *       :
 *       "&lt;objectm&gt;": [&lt;vm1&gt;, &lt;vm2&gt;, &lt;vm3&gt;, ... &lt;vmn&gt;]
 *     ]
 *   }
 * </pre>
 * Note that each value is a primitive item or an <em>array</em> of primitive items.
 *
 * @author <a href="mailto:jacob.dreyer@geosoft.no">Jacob Dreyer</a>
 */
public final class JsonTable
{
  /** Table name. Never null. */
  private final String name_;

  /** The attributes (i.e.&nbsp;columns) of the table. */
  private final List<String> attributes_ = new ArrayList<>();

  /** The objects (i.e.&nbsp;rows) and associated values per attribute. */
  private final Map<String, List<List<Object>>> objects_ = new LinkedHashMap<>();

  /**
   * Create a table with the specified name and the given set
   * of attributes.
   *
   * @param name        Table name. Non-null.
   * @param attributes  List of attributes (i.e.&nbsp;columns). Non-null.
   * @throws IllegalArgumentException  If name or attributes is null.
   */
  public JsonTable(String name, List<String> attributes)
  {
    if (name == null)
      throw new IllegalArgumentException("name cannot be null");

    if (attributes == null)
      throw new IllegalArgumentException("attributes cannot be null");

    name_ = name;
    attributes_.addAll(attributes);
  }

  /**
   * Create a JSON table as a copy of the specified one.
   *
   * @param table  Table to copy. Non-null.
   * @throws IllegalArgumentException  If table is null.
   */
  JsonTable(JsonTable table)
  {
    if (table == null)
      throw new IllegalArgumentException("table cannot be null");

    name_ = table.getName();
    attributes_.addAll(table.getAttributes());

    for (String objectName : table.getObjects()) {
      addObject(objectName);

      for (String attribute : attributes_) {
        List<Object> values = table.getValues(objectName, attribute);
        for (Object value : values)
          addValue(objectName, attribute, value);
      }
    }
  }

  /**
   * Create a JSON table from the specified JSON object.
   *
   * @param name        Table name. Non-null.
   * @param jsonObject  The JSON object to create table from. Non-null.
   * @return            The associated JSON table, or null if the JSON
   *                    object doesn't appear to be a table.
   * @throws IllegalArgumentException  If jsonObject is null.
   */
  static JsonTable create(String name, JsonObject jsonObject)
  {
    if (name == null)
      throw new IllegalArgumentException("name cannot be null");

    if (jsonObject == null)
      throw new IllegalArgumentException("jsonObject cannot be null");

    // Accept only the "attributes" and "objects" keys
    if (jsonObject.keySet().size() != 2)
      return null;

    //
    // Attributes
    //
    JsonValue attributesValue = jsonObject.get("attributes");
    if (attributesValue == null || attributesValue.getValueType() != JsonValue.ValueType.ARRAY)
      return null;

    List<String> attributes = new ArrayList<>();

    JsonArray attributesArray = (JsonArray) attributesValue;
    for (JsonValue attributeValue : attributesArray) {
      if (attributeValue.getValueType() != JsonValue.ValueType.STRING)
        return null;

      String attribute = ((JsonString) attributeValue).getString();

      attributes.add(attribute);
    }

    // Create the table
    JsonTable table = new JsonTable(name, attributes);

    //
    // Objects
    //
    JsonValue objectsValue = jsonObject.get("objects");
    if (objectsValue == null || objectsValue.getValueType() != JsonValue.ValueType.ARRAY)
      return null;

    JsonArray objectsArray = (JsonArray) objectsValue;
    for (JsonValue objectValue : objectsArray) {
      if (objectValue.getValueType() != JsonValue.ValueType.OBJECT)
        return null;

      JsonObject object = (JsonObject) objectValue;
      String objectName = JsonUtil.getKey(object);

      table.addObject(objectName);

      //
      // Values
      //
      JsonValue objectValues = object.get(objectName);
      if (objectValues.getValueType() != JsonValue.ValueType.ARRAY)
        return null;

      JsonArray valuesArray = (JsonArray) objectValues;
      if (valuesArray.size() != attributes.size())
        return null;

      for (int attributeNo = 0; attributeNo < attributes.size(); attributeNo++) {
        String attribute = attributes.get(attributeNo);
        JsonValue value = valuesArray.get(attributeNo);

        if (value.getValueType() == JsonValue.ValueType.ARRAY) {
          JsonArray subValues = (JsonArray) value;
          for (JsonValue subValue : subValues) {
            if (!JsonUtil.isPrimitive(subValue))
              return null;

            Object v = JsonUtil.getValue(subValue);

            table.addValue(objectName, attribute, v);
          }
        }
        else {
          table.addValue(objectName, attribute, JsonUtil.getValue(value));
        }
      }
    }

    return table;
  }

  /**
   * Check if the specified JSON object seems to have a table structure.
   *
   * @param jsonObject  JSON object to check. Non-null.
   * @return            True if the JSON object has the structure of a table,
   *                    false otherwise.
   * @throws IllegalArgumentException  If jsonObject is null.
   */
  static boolean isTable(JsonObject jsonObject)
  {
    if (jsonObject == null)
      throw new IllegalArgumentException("jsonObject cannot be null");

    return create("table", jsonObject) != null;
  }

  /**
   * Return name of this table.
   *
   * @return  Name of this table. Never null.
   */
  public String getName()
  {
    return name_;
  }

  /**
   * Return the attributes (i.e.&nbsp;columns) of this table.
   *
   * @return  The attributes of this table. Never null.
   */
  public List<String> getAttributes()
  {
    return Collections.unmodifiableList(attributes_);
  }

  /**
   * Return the objects (i.e.&nbsp;rows) of this table.
   *
   * @return  The objects of this table. Never null.
   */
  public Set<String> getObjects()
  {
    return Collections.unmodifiableSet(objects_.keySet());
  }

  /**
   * Check if this table contains the specified object.
   *
   * @param objectName  Name of object to check. Non-null.
   * @return True if the object is in the table, false otherwise.
   * @throws IllegalArgumentException  If objectName is null.
   */
  public boolean contains(String objectName)
  {
    return objects_.keySet().contains(objectName);
  }

  /**
   * Add an object (i.e.&nbsp;row) to this table.
   *
   * @param objectName  Name of object to add. Non-null.
   * @throws IllegalArgumentException  If object Name is null,
   *                    or the object already exists in this table.
   */
  public void addObject(String objectName)
  {
    if (objectName == null)
      throw new IllegalArgumentException("objectName cannot be null");

    if (contains(objectName))
      throw new IllegalArgumentException("Object already in table: " + objectName);

    List<List<Object>> values = new ArrayList<List<Object>>();
    for (int i = 0; i < attributes_.size(); i++)
      values.add(new ArrayList<Object>());

    objects_.put(objectName, values);
  }

  /**
   * Add the specified value to the given object/attribute.
   *
   * @param objectName  Name of object (i.e.&nbsp;row) to add value to. Non-null.
   * @param attribute   Attribute (i.e.&nbsp;column) to add value to. Non-null.
   * @param value       Value to add. Null for no-value.
   * @throws IllegalArgumentException  If objectName or attribute is null or
   *                    not present in the table.
   */
  public void addValue(String objectName, String attribute, Object value)
  {
    if (objectName == null)
      throw new IllegalArgumentException("objectName cannot be null");

    if (!contains(objectName))
      throw new IllegalArgumentException("Unknown object: " + objectName);

    if (attribute == null)
      throw new IllegalArgumentException("attribute cannot be null");

    if (!attributes_.contains(attribute))
      throw new IllegalArgumentException("Unknwon attribute: " + attribute);

    List<Object> values = getValues(objectName, attribute);
    values.add(value);
  }

  /**
   * Return the values of the specified object for the given attribute.
   *
   * @param objectName  Name of object to get values for.
   * @param attribute   Attribute to get values for.
   * @return            Requested values. Never null.
   * @throws IllegalArgumentException  If objectName or attribute is null or
   *                    not present in the table.
   */
  public List<Object> getValues(String objectName, String attribute)
  {
    if (objectName == null)
      throw new IllegalArgumentException("objectName cannot be null");

    if (!contains(objectName))
      throw new IllegalArgumentException("Unknown object: " + objectName);

    if (attribute == null)
      throw new IllegalArgumentException("attribute cannot be null");

    if (!attributes_.contains(attribute))
      throw new IllegalArgumentException("Unknwon attribute: " + attribute);

    int attributeNo = attributes_.indexOf(attribute);
    return objects_.get(objectName).get(attributeNo);
  }

  /**
   * Return (first) value of the specified object for the given attribute.
   * This is a convenience method if the client knows that the cell contains
   * exactly one value.
   *
   * @param objectName  Name of object to get values for.
   * @param attribute   Attribute to get values for.
   * @return            Requested values.
   * @throws IllegalArgumentException  If objectName or attribute is null or
   *                    not present in the table.
   */
  public Object getValue(String objectName, String attribute)
  {
    if (objectName == null)
      throw new IllegalArgumentException("objectName cannot be null");

    if (!contains(objectName))
      throw new IllegalArgumentException("Unknown object: " + objectName);

    if (attribute == null)
      throw new IllegalArgumentException("attribute cannot be null");

    if (!attributes_.contains(attribute))
      throw new IllegalArgumentException("Unknwon attribute: " + attribute);

    List<Object> values = getValues(objectName, attribute);
    return !values.isEmpty() ? values.get(0) : null;
  }

  /**
   * Return the content of this table as a JSON object.
   *
   * @return  The requested JSON object. Never null.
   */
  JsonObject asJsonObject()
  {
    JsonObjectBuilder tableObjectBuilder = Json.createObjectBuilder();

    //
    // Attributes
    //
    JsonArrayBuilder attributesArrayBuilder = Json.createArrayBuilder();
    for (String attribute : attributes_)
      attributesArrayBuilder.add(attribute);
    tableObjectBuilder.add("attributes", attributesArrayBuilder);

    //
    // Objects
    //
    JsonObjectBuilder objectsBuilder = Json.createObjectBuilder();
    for (Map.Entry<String,List<List<Object>>> entry : objects_.entrySet()) {
      String objectName = entry.getKey();

      //
      // Values
      //
      List<List<Object>> values = entry.getValue();

      JsonArrayBuilder valuesArrayBuilder = Json.createArrayBuilder();

      for (List<Object> v : values) {
        if (v.isEmpty())
          JsonUtil.add(valuesArrayBuilder, null);
        else if (v.size() == 1) {
          JsonUtil.add(valuesArrayBuilder, v.get(0));
        }
        else {
          JsonArrayBuilder subValuesArrayBuilder = Json.createArrayBuilder();
          for (Object object : v)
            JsonUtil.add(subValuesArrayBuilder, object);

          valuesArrayBuilder.add(subValuesArrayBuilder);
        }
      }

      objectsBuilder.add(objectName, valuesArrayBuilder);
    }
    tableObjectBuilder.add("objects", objectsBuilder);

    return tableObjectBuilder.build();
  }

  /** {@inheritDoc} */
  @Override
  public String toString()
  {
    StringBuilder s = new StringBuilder();

    //
    // Determine column widths
    //
    int nColumns = attributes_.size() + 1;
    int[] columnWidth = new int[nColumns];

    for (Map.Entry<String, List<List<Object>>> entry : objects_.entrySet()) {
      String objectName = entry.getKey();
      if (objectName.length() > columnWidth[0])
        columnWidth[0] = objectName.length();
    }

    for (int columnNo = 1; columnNo < nColumns; columnNo++) {
      String attribute = attributes_.get(columnNo - 1);
      if (attribute.length() > columnWidth[columnNo])
        columnWidth[columnNo] = attribute.length();

      for (List<List<Object>> values : objects_.values()) {
        List<Object> attributeValues = values.get(columnNo - 1);
        for (Object v : attributeValues) {
          String text = v != null ? v.toString() :  "null";
          if (text.length() > columnWidth[columnNo])
            columnWidth[columnNo] = v.toString().length();
        }
      }
    }

    // Total table width
    int totalWidth = nColumns - 1;
    for (int width  : columnWidth)
      totalWidth += width;

    //
    // tableName
    //
    s.append(name_);
    s.append('\n');

    //
    //          attr1  attr2  attr3  ...  attrn
    //
    s.append(Util.getSpaces(columnWidth[0]));
    s.append(' ');
    for (int columnNo = 1; columnNo < nColumns; columnNo++) {
      String attribute = attributes_.get(columnNo - 1);
      String formatString = "%-" + columnWidth[columnNo] + "s";
      s.append(String.format(formatString, attribute));
      s.append(' ');
    }
    s.append('\n');

    //
    // ----------------------------------------
    //
    for (int i = 0; i < totalWidth; i++)
      s.append('-');
    s.append('\n');

    //
    // object1  value1 value2 value3 ... valuen
    // object2  value1 value2 value3 ... valuen
    //    :
    // objectm  value1 value2 value3 ... valuen
    //
    for (Map.Entry<String,List<List<Object>>> entry : objects_.entrySet()) {
      String objectName = entry.getKey();
      List<List<Object>> values = entry.getValue();

      // Number of rows for this particular object
      int nRows = 1;
      for (List<Object> v : values) {
        if (v.size() > nRows)
          nRows = v.size();
      }

      // Append each row
      for (int rowNo = 0; rowNo < nRows; rowNo++) {
        for (int columnNo = 0; columnNo < nColumns; columnNo++) {

          String formatString = "%-" + columnWidth[columnNo] + "s";
          String empty = Util.getSpaces(columnWidth[columnNo]);

          if (columnNo == 0)
            s.append(rowNo == 0 ? String.format(formatString, objectName) : empty);
          else {
            List<Object> attributeValues = values.get(columnNo - 1);
            boolean hasValue = rowNo < attributeValues.size();
            Object v = hasValue ? attributeValues.get(rowNo) : null;
            if (!hasValue)
              s.append(empty);
            else
              s.append(v != null ? String.format(formatString, v.toString()) : "null");
          }

          s.append(' ');
        }
        s.append('\n');
      }
    }

    //
    // ----------------------------------------
    //
    for (int i = 0; i < totalWidth; i++)
      s.append('-');
    s.append('\n');


    return s.toString();
  }
}
