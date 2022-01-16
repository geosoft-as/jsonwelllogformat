package no.geosoft.jwlf;

/**
 * Provides a mechanism for the client to monitor and process data
 * <em>during</em> a JSON read operation, and also to abort the
 * process in case that is requested by user or for other reasons.
 * <p>
 * Convenient for handling JSON content that are larger than physical
 * memory. In this case the client should <em>clear</em> the log
 * instance at fixed intervals:
 *
 * <blockquote>
 *   <pre>
 *   class DataListener implements JsonDataListener
 *   {
 *     &#64;Override
 *     public void dataRead(JsonLog log)
 *     {
 *       // Process log data
 *       :
 *
 *       // Clear curve data to save memory
 *       log.clearCurves();
 *
 *       // Continue the process
 *       return true;
 *     }
 *   }
 *   </pre>
 * </blockquote>
 *
 * @author <a href="mailto:jacob.dreyer@geosoft.no">Jacob Dreyer</a>
 */
public interface JsonDataListener
{
  /**
   * A notification from {@link JsonReader} indicating that a new
   * portion of data has been read into the specified JSON file.
   * <p>
   * After the client has processed the data, it may clean the curve data
   * in order to save memory storage. See {@link JsonLog#clearCurves}.
   * <p>
   * It is also possible for the client to <em>abort</em> the reading
   * process at this time, by returning <tt>false</tt> from the method.
   * This will close all resources and throw an InterruptedException
   * back to the client.
   * <p>
   * @see JsonReader#read(boolean,boolean,JsonDataListener)
   *
   * @param log  Log that has been populated with new data. Never null.
   * @return     True to continue reading, false to abort the process.
   */
  public boolean dataRead(JsonLog log);
}
