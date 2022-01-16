# JSON Well Log Format

<img hspace="100" src="https://petroware.no/images/json.png">

## Setup

Capture the JSON Well Log Format GitHub content to local disk by:

```
$ git clone https://github.com/JSONWellLogFormat/JSONWellLogFormat.git
```

## Background

Most well log and drilling data in the oil and gas industry is trapped within tapes and disk files of ancient and hard to access data formats like DLIS, LAS, LIS, BIT, XTF, WITS, ASC and SPWLA.

These formats represents _orphaned_ technologies and are outdated in all possible ways. Their syntax is overly complex, convoluted and awkward, available support software is limited, software tools are rare and documentation is poor or nonexistent.

But still: These are the main storage and communication media for well logging information in the 2020s. The amount of data is immense and growing, as is the aggregate cost of maintaining and utilizing this information.


## The JSON Well Log Format

The JSON Well Log Format is a modern well log format designed for the future requirements of simplicity, compatibility, speed, massive storage, massive transmission, cloud computing and big data analytics. It overcome many of the deficiencies of existing well log formats.

* Based on the _JavaScript Object Notation_ ([JSON](https://www.json.org/)) open standard ([RFC 8259](https://tools.ietf.org/html/rfc8259) and [RFC 7493](https://tools.ietf.org/html/rfc7493))
* Non-proprietary
* Text-based, lightweight and human readable
* Full [UTF-8](https://en.wikipedia.org/wiki/UTF-8) support according to the JSON standard
* Built-in _no-value_ support
* Simple syntax consisting of collections of name/value pairs (_objects_) and ordered lists of values (_arrays_)
* Compact type system
* Quantity and unit support based on the [Unit of Measure Standard](https://www.energistics.org/energistics-unit-of-measure-standard/) from [Energistics](https://energistics.org)
* Date and time support through the [ISO 8601](https://www.iso.org/iso-8601-date-and-time-format.html) standard
* Well log semantics based on a limited set of _well known_ keys to ensures consistency, compatibility and efficient processing
* Supports depth and time based logging data
* Supports single value and multi-dimensional (image) curves
* Fast: The simple syntax and streaming nature makes parsing extremely efficient
* Omnipresent parsers and generators for just about [_any_](http://json.org/) system environment available
* Existing ecosystem of NoSQL cluster database support with high volume storage, search and indexing, distribution, scalability and high performance analytics


## Example

A JSON Well Log file consists of one or more log sets each containing a log header, curve definitions and the corresponding measurement data. This example contains a single log set with two one-dimensional curves:

```json
[
  {
    "header": {
      "name": "EcoScope Data",
      "well": "35/12-6S",
      "field": "Fram",
      "date": "2022-06-14",
      "operator": "GeoSoft",
      "startIndex": 2907.79,
      "endIndex": 2907.84,
      "step": 0.01
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
        "name": "A40H",
        "description": "Attenuation resistivity 40 inch",
        "quantity": "electrical resistivity",
        "unit": "ohm.m",
        "valueType": "float",
        "dimensions": 1
      }
    ],
    "data": [
      [2907.79, 29.955],
      [2907.80, 28.892],
      [2907.81, 27.868],
      [2907.82, 31.451],
      [2907.83, 28.080],
      [2907.84, 27.733]
    ]
  }
]
```

The JSON syntax can be efficiently parsed in [any](http://json.org) programming environment available. The well log _semantics_ must still be parsed by the client code, but this is far simpler to do navigating in-memory data structures in the programming environment at hand, instead of dealing with external disk resources of obscure proprietary formats.


## Data types

The JSON Well Log Format defines the following data types for header data and curves:


| Type         | Description                    | Examples                                        |
|--------------|--------------------------------|-------------------------------------------------|
| **float**    | Floating point decimal numbers | 10.2, 0.014, 3.1e-108, 2.13e12, 0.0, null       |
| **integer**  | Integer decimal numbers        | 10, 42, 1000038233, -501, null                  |
| **string**   | Text strings                   | "error", "final depth", "message 402", "", null |
| **datetime** | Date/time specifications according to [ISO 8601](https://www.iso.org/iso-8601-date-and-time-format.html) | "2019-12-19", "2010-02-18T16:23:48,3-06:00", null |
| **boolean**  | Logic states                   | true, false, null                               |

Numbers must contain values corresponding to a double-precision 64-bit [IEEE 754](https://en.wikipedia.org/wiki/IEEE_754) binary format value. Integer values has the same internal representation in JavaScript as floats and should be limited to 52 bits (+/-9007199254740991) to ensure accuracy.

Also, numeric values that cannot be represented as sequences of digits (such as _Infinity_ and _NaN_) must be avoided.


## Log header

The log header contains metadata that describes the overall logging operation and consists of any JSON objects and arrays that the producing entity find necessary and sufficient.

However, in order to efficiently communicate metadata across disparate systems and companies the common properties listed below are defined as _well known_. Metadata outside this set has low informational value and is in general not fit for further processing.

| Key                | Type                             | Description                           |
|--------------------|----------------------------------|---------------------------------------|
| **name**           | string                           | Log name                              |
| **description**    | string                           | Log description                       |
| **externalIds**    | object of key,value string pairs | IDs within external storage, _key_ being the storage name, and _value_ being the ID. |
| **well**           | string                           | Well name                             |
| **wellbore**       | string                           | Wellbore name                         |
| **field**          | string                           | Field name                            |
| **country**        | string                           | Country of operation                  |
| **date**           | datetime                         | Logging date                          |
| **operator**       | string                           | Operator company name                 |
| **serviceCompany** | string                           | Service company name                  |
| **runNumber**      | string                           | Run number                            |
| **elevation**      | float                            | Vertical distance between measured depth 0.0 and _mean sea level_ in SI unit (meters). |
| **source**         | string                           | Source system or process of this log  |
| **startIndex**     | _According to index value type_  | Value of the first index. Unit according to index curve. |
| **endIndex**       | _According to index value type_  | Value of the last index. Unit according to index curve. |
| **step**           | _According to index value type_  | Distance between indices if regularly sampled. Unit according to index curve. If log is time based, milliseconds assumed. |
| **dataUri**        | string                           | Point to data source in case this is kept separate. Can be absolute or relative according to the [URI](https://en.wikipedia.org/wiki/Uniform_Resource_Identifier) specification. |

All header data are optional including the header object itself.

Please note that there is no perfect set of header information suiting all clients, and to avoid lengthy discussions on the topic the _well known_ part of the header is deliberately kept at a minimum.


## Curve definition

The following keys are used for curve definitions:

| Key                 | Type                      | Description                                                              |
|---------------------|---------------------------|--------------------------------------------------------------------------|
| **name**            | string                    | Curve name or mnemonic. Mandatory. Non-null.                             |
| **description**     | string                    | Curve description. Optional.                                             |
| **quantity**        | string                    | Curve quantity such as _length_, _pressure_, _force_ etc. Optional.      |
| **unit**            | string                    | Unit of measurement such as _m_, _ft_, _bar_, etc. Optional.             |
| **valueType**       | string                    | Curve value type: _float_, _integer_, _string_, _datetime_ or _boolean_. Non-null. Optional. _float_ assumed if not present. |
| **dimensions**      | integer                   | Number of dimensions. [1,>. Non-null. Optional. 1 assumed if not present.|
| **axis**            | array of curve definition | A detailed description of the multi-dimensional structure of the curve in case this spans multiple _axes_. One element per axis. The combined product of the axis ```diemsnsions``` elements must equal the dimensions of the curve. Optional. |
| **maxSize**         | integer                   | Maximum storage size (number of bytes) for UTF-8 string data. Used with binary storage in order to align the curve data. [0,>. Optional. 20 assumed if not present. Ignored for curves where ```valueType``` is other than string. |

Quantities and units should follow the [Unit of Measure Standard](https://www.energistics.org/energistics-unit-of-measure-standard/) from Energistics. To ease transition from legacy formats this is no requirement.

In addition to the listed, clients may add any number of _custom_ curve definition entries in any form supported by the JSON syntax, but as for header data in general this is not recommended.


## Curve data

Curve data are specified in arrays for each index entry, with one entry per curve. If a curve is multi-dimensional, the entry is itself an array of subentries, one per dimension.

Curve values are according to the value type defined for the curve, or `null` for no-values. The index curve is always the first curve listed, and must not contain no-values. It is advised that the index curve is continuously increasing or decreasing.

No custom additions to the curve defintion may alter the _structure_ of the data definition as specified above.


## Transition objects

To support convertion of legacy formats to JSON a generic _table_ object has been suggested. The table has a set of _attributes_ and it contains a number of named _objects_ with one or more _values_ for each attribute. The table is able to represent metadata from existing well log formats in a consistent and simple manner:

```txt
"name": {
  "attributes": ["attr1", "attr2", "attr3", ... "attrn"],
  "objects": {
    "object1": [v11, v12, v13, ... v1n],
    "object2": [v21, v22, v23, ... v2n],
    "object3": [v31, v32, v33, ... v3n],
    :
    "objectm": [vm1, vm2, vm3, ... vmn]
  }
}
```

Metedata in _LAS_ files exists as _parameters_ within a _section_ and has the following form:

```txt
<name>.<unit> <value> : <description>
```

A typical example might be:

```txt
~PARAMETER INFORMATION
#MNEM.UNIT    VALUE                      DESCRIPTION
#---- -----   --------------------       ------------------------
RUN .   1A        : RUN NUMBER
PDAT.   MSL       : Permanent Datum
EPD .C3  0.000000 : Elevation of Permanent Datum above Mean Sea Level
LMF .   DF        : Logging Measured From (Name of Logging Elevation Reference)
APD .M  30.000000 : Elevation of Depth Reference (LMF) above Permanent Datum
```

Using the table object above, this should convert to JSON as follows:

```json
"PARAMETER INFORMATION": {
  "attributes": ["value", "unit", "description"],
  "objects": {
    "RUN":  ["1A",  null, "RUN NUMBER"],
    "PDAT": ["MSL", null, "Permanent Datum"],
    "EPD":  [0.0,   "C3", "Elevation of Permanent Datum above Mean Sea Level"],
    "LMF":  ["DF",  null, "Logging Measured From (Name of Logging Elevation Reference)"],
    "APD":  [30.0,  "M",  "Elevation of Depth Reference (LMF) above Permanent Datum"]
  }
}
```

Metedata in _DLIS_ files exists as _sets_. This is a named entity with a number of attributes and a number of objects with one or more values for each of the attributes. A DLIS set has a binary representation within a DLIS file, but it can be viewed as a matrix as follows:

```txt
setName
         attr1  attr2  attr3  ... attrn
----------------------------------------
object1    v11    v12    v13        v1n
object2    v21    v22    v23        v2n
object3    v31    v32    v33        v3n
   :
objectm    vm1    vm2    vm3        vmn
----------------------------------------
```

A typical example might be:

```txt
HzEquipment
          LENGTH   TRADEMARK-NAME SERIAL-NUMBER WEIGHT
---------------------------------------------------------
APWD      0.0 in   APWD25-AA      241408        0.0 kg
ARDC      224.8 in ARC9D-BA       738           1270.0 kg
MSSD900   14.5 in  SZR            FC-71545      68.0 kg
---------------------------------------------------------
```

Using the generic table structure, this will convert to JSON as follows:

```json
"HzEquipment": {
  "attributes": ["LENGTH", "TRADEMARK-NAME", "SERIAL-NUMBER", "WEIGHT"],
  "objects": {
    "APWD": ["0.0 in", "APWD25-AA", "241408", "0.0 kg"],
    "ARDC": ["224.8 in", "ARC9D-BA", "738", "1270.0 kg"],
    "MSSD900": ["14.5 in", "SZR", "FC-71545", "68.0 kg"]
  }
}
```


## Writing JSON well logs

Writing JSON well logs can be done in two different formats: _condensed_ or _pretty_. The condensed format should be without whitespace and newlines and should be used for transmission between computers only.

For well logs that may possibly be viewed by humans the pretty format should always be used. This format should contain whitespace and indentation that emphasizes the logical structure of the content. For the data section in particular, arrays of curve data for each index must be written _horizontally_ and commas between entries should be _aligned_:

```txt
"data": [
    [828420, 282.589,  8.6657, 2.202, 2.222, [1.759, 2.31469,  1.33991E-3, 3.75839], 0.52435, ... ],
    [828480, 286.239,  9.6601, 2.277, 2.297, [2.219, 2.31189,        null,    null], 0.52387, ... ],
    [828540, 276.537, 10.6638, 2.309,  null, [2.267, 2.29509, -3.67117E-3,    null], 0.53936, ... ],
    [828600, 264.325, 10.6545, 2.324,  null, [2.110, 2.27902, -7.77555E-3, 3.67927], 0.55439, ... ],
    [828660, 245.938,  9.6937, 2.333, 2.356, [1.525, 2.26512, -1.17965E-2, 3.68386], 0.56211, ... ],
    :
]
```


## Binary storage

The _data_ object of JSON Well Log Format file may optionally be stored in a separate binary file. The location of the file must be specified in the ```dataUri``` property of the header.

The binary format is without structure, it just lists the curve values row by row. This allows for extremely fast access along any axis of the data.

The binary storage format for each value type is described below:

| Type        | Storage                                                                                                                                                                        | No-value                                                     |
|-------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------|
| **float**   | 64 bit [IEEE 754](https://en.wikipedia.org/wiki/IEEE_754) floating point                                                                                                       | IEEE 754 NaN                                                 |
| **integer** | 64 bit, [big endian](https://en.wikipedia.org/wiki/Endianness)                                                                                                                 | 2<sup>63</sup> - 1, being the largest possible 64 bit number |
| **string**  | [UTF-8](https://en.wikipedia.org/wiki/UTF-8) encoded text, left aligned, space padded, ```maxSize``` _bytes_ (or 20 if not specified)                                          | Empty string                                                 |
| **boolean** | 8 bit, 0 = false, 1 = true                                                                                                                                                     | Any value different from 0 and 1                             |
| **datetime**| [ASCII](https://en.wikipedia.org/wiki/ASCII) encoded text containing [ISO 8601](https://www.iso.org/iso-8601-date-and-time-format.html) date/time specification, 30 characters | Empty string                                                 |


## Version

To ensure stability, The JSON Well Log Format is _unversioned_.

The set of _well known_ header information may possibly be extended over time, but the structure of the format as such will not change.


## Schema

Schema for the JSON Well Log Format is available at [https://jsonwelllogformat.org/schemas/JsonWellLogFormat.json](https://jsonwelllogformat.org/schemas/JsonWellLogFormat.json).


## Examples - The Volve data set

Thanks to [Equinor](http://equinor.com) all subsurface and production data from the Volve field on the Norwegian continental shelf has been disclosed and made available to the public. It can be downloaded from [http://data.equinor.com](http://data.equinor.com).

The dataset contains about 15GB of well log data in about 1000 different DLIS, LIS, LAS, ASC and SPWLA files. This part has been converted to the JSON Well Log Format by [Petroware](https://petroware.no) and is available [here](https://jsonwelllogformat.org/Volve).

[JavaScript Code](https://github.com/JSONWellLogFormat/JSONWellLogFormat/tree/master/JavaScript) to view JSON Well Log Format data in a web browser has been contributed and is available [here](https://jsonwelllogformat.org/viewer/index.html).
