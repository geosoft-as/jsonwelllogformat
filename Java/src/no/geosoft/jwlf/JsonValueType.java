package no.geosoft.jwlf;

import java.util.Date;

/**
 * Represents the different data types defined in the JSON Well Log Format.
 *
 * @author <a href="mailto:jacob.dreyer@geosoft.no">Jacob Dreyer</a>
 */
enum JsonValueType
{
  /** Floating point decimal numbers. */
  FLOAT("float", Double.class),

  /** Integer decimal numbers. */
  INTEGER("integer", Long.class),

  /** Text strings. */
  STRING("string", String.class),

  /** Boolean (true/false) values. */
  BOOLEAN("boolean", Boolean.class),

  /** Date and time. */
  DATETIME("datetime", Date.class);

  /** Name of this value type as it will appear within JSON files. Non-null. */
  private final String name_;

  /** The equivalent Java class of this value type. Non-null. */
  private final Class<?> valueType_;

  /**
   * Create a JSON value type as defined by the JSON Well Log Format.
   *
   * @param name       Value type name as it will appear on file. Non-null.
   * @param valueType  Equivalent Java class. Non-null.
   */
  private JsonValueType(String name, Class<?> valueType)
  {
    assert name != null : "name cannot be null";
    assert valueType != null : "valueType cannot be null";

    name_ = name;
    valueType_ = valueType;
  }

  /**
   * Return the name of this value type.
   *
   * @return  Name of this value type. Never null.
   */
  String getName()
  {
    return name_;
  }

  /**
   * Return the Java class value type of this value type.
   *
   * @return  The Java class value type of this value type. Never null.
   */
  Class<?> getValueType()
    {
    return valueType_;
  }

  /**
   * Return value type for the specified JSON type name.
   *
   * @param name  Name to get value type for. Non-null.
   * @return      Associated JSON value type. Null if not found.
   */
  static JsonValueType get(String name)
  {
    assert name != null : "name cannot be null";

    for (JsonValueType valueType : JsonValueType.values())
      if (valueType.getName().equals(name))
        return valueType;

    // Not found
    return null;
  }

  /**
   * Return value type for the specified Java class.
   *
   * @param clazz  Java class to get value type for. Non-null.
   * @return       Associated JSON value type. Null if not found.
   */
  static JsonValueType get(Class<?> clazz)
  {
    assert clazz != null : "clazz cannot be null";

    // Search for exact match
    for (JsonValueType valueType : JsonValueType.values()) {
      if (valueType.getValueType() == clazz)
        return valueType;
    }

    //
    // Search the obvious relations
    //

    if (clazz == Double.class)
      return FLOAT;

    if (clazz == Float.class)
      return FLOAT;

    if (clazz == Byte.class)
      return INTEGER;

    if (clazz == Short.class)
      return INTEGER;

    if (clazz == Integer.class)
      return INTEGER;

    if (clazz == Long.class)
      return INTEGER;

    //
    // Anything else is classified as floating point numbers
    //
    return FLOAT;
  }

  /** {@inheritDoc} */
  @Override
  public String toString()
  {
    return name_;
  }
}
