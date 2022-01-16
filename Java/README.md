# JWLF - Java library for accessing JSON Well Log Format files

JWLF is an open source library from GeoSoft for reading and writing JWLF log files.


## Dependencies

The JWLF library depends on the JSON API specification and an implementation of this:

```
lib/javax.json-api-1.1.3.jar
lib/javax.json-1.1.3.jar
```

The JWLF jar file as well as its dependencies are available [here](https://github.com/JSONWellLogFormat/JSONWellLogFormat/tree/master/Java/lib).


## API Documentation

API documentation (JavaDoc) is available [here]().


## Programming examples

### Read

Example for reading a JSON Well Log file:

```java
import no.geosoft.jwlf.JsonLog;
import no.geosoft.jwlf.JsonReader;

:

// Create a JSON Well Log reader and read specified file to memory
JsonReader jsonReader = new JsonReader(new File("path/to/file.JSON"));
List<JsonLog> jsonLogs = jsonReader.read(true, null);
```

From this point the `JsonLog` instance(s) can be navigated to access curves
and header data.

If files are larger than physical memory it is possible to process the content
in a streaming manner by adding a `JsonDataListener` to the `read()` call.


### Validate

Example for validating a JSON Well Log file:

```java
import no.geosoft.jwlf.JsonValidator;

:

JsonValidator jsonValidator = JsonValidaor.getInstance();
List<JsonValidator.Message> messages = jsonValidator.validate(new File("path/to/file.JSON"));
```

The messages are of different severity and points to potential
problems at specific locations in the input file.


### Write

Example for creating a JSON Well Log file from scratch:

```java
import no.geosoft.jwlf.JsonCurve;
import no.geosoft.jwlf.JsonLog;
import no.geosoft.jwlf.JsonWriter;

:

// Create an empty JSON log instance
JsonLog jsonLog = new JsonLog();

// Populate with metadata
jsonLog.setName("EcoScope Data");
jsonLog.setWell("35/12-6S");
jsonLog.setField("Ekofisk");
:

// Create curves
JsonCurve c1 = new JsonCurve("MD", "Measured depth", "length", "m", Double.class, 1);
jsonLog.addCurve(c1);

JsonCurve c2 = new JsonCurve("RES", "Resistivity", "electrical resistivity", "ohm.m", Double.class, 1);
jsonLog.addCurve(c2);

// Add curve data
c1.addValue(1000.0);
c1.addValue(1001.0);
c1.addValue(1002.0);
:
c1.addValue(1349.0);

c2.addValue(127.3);
c2.addValue(92.16);
c2.addValue(null);
:
c2.addValue(118.871);

// Specify metadata for index
jsonLog.setStartIndex(jsonLog.getActualStartIndex());
jsonLog.setEndIndex(jsonLog.getActualEndIndex());
jsonLog.setStep(jsonLog.getActualStep());

// Write to file, human readable with 2 space indentation
JsonWriter jsonWriter = new JsonWriter(new File("path/to/file.json", true, 2);
jsonWriter.write(jsonLog);
jsonWriter.close();
```

This will produce the following file:

```
[
  {
    "header": {
      "name": "EcoScope Data" ,
      "well": "35/12-6S",
      "field": "Ekofisk",
      "startIndex": 1000.0,
      "endIndex": 1349.0,
      "step": 1.0
    },
    "curves": [
      {
        "name": "MD",
        "description": "Measured depth",
        "quantity": "length",
        "unit": "m",
        "valueType": "float",
        "dimensions": 1
      },
      {
        "name": "RES",
        "description": "Resistivity",
        "quantity": "electrical resistivity",
        "unit": "ohm.m",
        "valueType": "float",
        "dimensions": 1
      }
    ]
    "data": [
      [1000.0, 127.300],
      [1001.0,  92.160],
      [1002.0,    null],
      :
      :
      [1349.0, 112.871]
    ]
  }
]
```
