package no.geosoft.jwlf;

/**
 * List the well known properties of JSON Well Log Format files.
 *
 * @author <a href="mailto:jacob.dreyer@geosoft.no">Jacob Dreyer</a>
 */
public enum JsonWellLogProperty
{
  /** Log name. */
  NAME("name"),

  /** Log description. */
  DESCRIPTION("description"),

  /** Well name. */
  WELL("well"),

  /** Wellbore name. */
  WELLBORE("wellbore"),

  /** Field name. */
  FIELD("field"),

  /** Country name. */
  COUNTRY("country"),

  /** Logging date. */
  DATE("date"),

  /** Operator company name. */
  OPERATOR("operator"),

  /** Service company name. */
  SERVICE_COMPANY("serviceCompany"),

  /** Run number. */
  RUN_NUMBER("runNumber"),

  /** Elevation. */
  ELEVATION("elevation"),

  /** Source system or process of this log. */
  SOURCE("source"),

  /** Start index. */
  START_INDEX("startIndex"),

  /** End index. */
  END_INDEX("endIndex"),

  /** Step if regular sampling. */
  STEP("step"),

  /** Pointer to data source in case this is kept separate. */
  DATA_URI("dataUri");

  /** Key used when the property is written to file. Non-null. */
  private final String key_;

  /**
   * Create a well known well log property entry.
   *
   * @param key  Key as when written to file. Non-null.
   */
  private JsonWellLogProperty(String key)
  {
    assert key != null : "key cannot be null";
    key_ = key;
  }

  /**
   * Return key of this property.
   *
   * @return Key of this property. Never null.
   */
  public String getKey()
  {
    return key_;
  }

  /**
   * Get property for the specified key.
   *
   * @param key  Key to get property of. Non-null.
   * @return     The associated property, or null if not found.
   * @throws IllegalArgumentException  If key is null.
   */
  public static JsonWellLogProperty getByKey(String key)
  {
    if (key == null)
      throw new IllegalArgumentException("key cannot be null");

    for (JsonWellLogProperty property : JsonWellLogProperty.values()) {
      if (property.getKey().equals(key))
        return property;
    }

    // Not found
    return null;
  }


  /** {@inheritDoc} */
  @Override
  public String toString()
  {
    return key_;
  }
}
