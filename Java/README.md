# Log I/O - Library for accessing well log files

Log I/O is a library from Petroware AS for reading and writing well log files.

As of Q1/2019 Log I/O supports DLIS, LIS, LAS 2.0, LAS 3.0, BIT, XTF, ASC,
SPWLA, CSV, and JSON Well Log Format. Log I/O wraps the complexity of these
formats in a clean, complete, well documented, efficient and extremely simple
to use programming API.

<img hspace="100" src="https://petroware.no/images/LogIoBox.250.png">

The [https://github.com/Petroware/LogIo](open source) version of Log I/O contains
the Java accessor and validator for the JSON Well Log Format.

Log I/O web page: https://petroware.no/logio.html



## Setup

Capture the Log I/O code to local disk by:

```
$ git clone https://github.com/Petroware/LogIo.git
```



## Dependencies

The JSON Well Log Format accessor depends on the JSON API specification
and an implementation of this:

```
lib/javax.json-api-1.1.3.jar
lib/javax.json-1.1.3.jar
```



## API Documentation

Java: https://petroware.no/logio/javadoc/index.html



## Programming examples


### Read

Example for reading a JSON Well Log file:

```java
import no.petroware.logio.json.JsonLog;
import no.petroware.logio.json.JsonReader;

:

// Create a JSON Well Log reader and read specified file to memory
JsonReader jsonReader = new JsonReader(new File("path/to/file.JSON"));
List<JsonLog> jsonLogs = jsonReader.read(true, false, null);
```

From this point the `JsonLog` instance(s) can be navigated to access curves
and header data.

If files are larger than physical memory it is possible to process the content
in a streaming manner by adding a `JsonDataListener` to the `read()` call.


### Validate

Example for validating a JSON Well Log file:

```java
import no.petroware.logio.json.JsonValidator;

:

JsonValidator jsonValidator = JsonValidaor.getInstance();
List<JsonValidator.Message> messages = jsonValidator.validate(new File("path/to/file.JSON"));
```

The messages are of different severity and points to potential
problems at specific locations in the input file.


### Write

Example for creating a JSON Well Log file from scratch:

```java
import no.petroware.logio.json.JsonCurve;
import no.petroware.logio.json.JsonLog;
import no.petroware.logio.json.JsonWriter;

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

