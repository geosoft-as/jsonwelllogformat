# JSON Well Log Format

<img hspace="100" src="https://petroware.no/images/json.png">

## Background

Most well log and drilling data in the oil and gas industry is trapped within tapes and disk files of ancient and hard to access data formats like DLIS, LAS, LIS, BIT, XTF, WITS, ASC and SPWLA.

These formats represents _orphaned_ technologies and are outdated in all possible ways. Their syntax is overly complex, convoluted and awkward, available support software is limited, software tools are rare and documentation is poor or nonexistent.

But still: These are the main storage and communication media for well logging information in the 2018. The amount of data is immense and growing, as is the aggregate cost of maintaining and utilizing this information.

## The JSON Well Log Format

The JSON Well Log Format is a modern well log format designed for the future requirements of simplicity, compatibility, speed, massive storage, massive transmission, cloud computing and big data analytics. It overcome many of the deficiencies of existing well log formats.

* Based on the _JavaScript Object Notation_ ([JSON](https://www.json.org/)) open standard ([RFC 8259](https://tools.ietf.org/html/rfc8259) and and [RFC 7493](https://tools.ietf.org/html/rfc7493)
* Text-based, lightweight and human readable
* Full [UTF-8](https://en.wikipedia.org/wiki/UTF-8) support according to the JSON standard
* Date and time support through the [ISO 8601](https://www.iso.org/iso-8601-date-and-time-format.html) standard
* Quantity and unit support based on the [Unit of Measure Standard](https://www.energistics.org/energistics-unit-of-measure-standard/) from [Energistics](https://energistics.org)
* Built-in _no-value_ support
* Simple syntax consisting of collections of name/value pairs (_objects_) and ordered lists of values (_arrays_)
* Well log semantics based on a limited set of _well known_ keys to ensures consistency, compatibility and efficient processing
* Compact type system
* Supports depth and time based logging data
* Supports single value and multi-dimensional (image) curves
* Fast: The simple syntax and streaming nature makes parsing extremely efficient
* Omnipresent parsers and generators for just about [_any_](http://json.org/) system environment available
* Existing ecosystem of NoSQL cluster database support with high volume storage, search and indexing, distribution, scalability and high performance analytics

## Example

A JSON Well Log file consists of one or more log sets each containing logging meta data, curve definitions and the corresponding measurement data. This example contains a single log set with two one-dimensional curves:

```json
{
  "logs": [
    {
      "metadata": {
        "name": "EcoScope Data",
        "well": "35/12-6S",
        "field": "Fram",
        "date": "2019-06-14",
        "operator": "Logtek Petroleum",
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
}
```

The JSON syntax can be efficiently parsed in [any](http://json.org) programming environment available. The well log _semantics_ must still be parsed by the client code, but this is far simpler to do navigating in-memory data structures in the programming environment at hand, instead of dealing with external disk resources of obscure proprietary formats.

## Data types

The JSON Well Log Format defines the following data types for metadata and curves:


| Type         | Description                    | Examples                                        |
|--------------|--------------------------------|-------------------------------------------------|
| **float**    | Floating point decimal numbers | 10.2, 0.014, 3.1e-108, 2.13e12, 0.0, null       |
| **integer**  | Integer decimal numbers        | 10, 42, 1000038233, -501, null                  |
| **string**   | Text strings                   | "error", "final depth", "message 402", "", null |
| **datetime** | Date/time specifications according to [ISO 8601](https://www.iso.org/iso-8601-date-and-time-format.html) | "2019-12-19", "2010-02-18T16:23:48,3-06:00", null |
| **boolean**  | Logic states                   | true, false, null                               |

Numbers must contain values corresponding to a double-precision 64-bit [IEEE 754](https://en.wikipedia.org/wiki/IEEE_754) binary format value. Integer values has the same internal representation in JavaScript as floats and should be limited to 52 bits (+/-9007199254740991) to ensure accuracy.

Also, numeric values that cannot be represented as sequences of digits (such as _Infinity_ and _NaN_) must be avoided.

## Logging metadata

The following metadata keys are defined as _well known_:

| Key                | Type     | Description           |
|--------------------|----------|-----------------------|
| **name**           | string   | Log name              |
| **description**    | string   | Log description       |
| **well**           | string   | Well name             |
| **wellbore**       | string   | Wellbore name         |
| **field**          | string   | Field name            |
| **country**        | string   | Country of operation  |
| **date**           | datetime | Logging date          |
| **operator**       | string   | Operator company name |
| **serviceCompany** | string   | Service company name  |
| **runNumber**      | string   | Run number            |
| **startInde**      | _According to index value type_  | Value of the first index. Unit according to index curve. |
| **endIndex**       | _According to index value type_  | Value of the last index. Unit according to index curve. |
| **step**           | _According to index value type_  | Distance between indices if regularly sampled. Unit according to index curve. If log is time based, milliseconds assumed. |

All metadata are optional.

In addition to the listed entries, clients may add any number of _custom_ metadata in any form supported by the JSON syntax. Note that the general informational value of custom metadata is _low_. Some clients may understand the meaning of the entries, but in general such information is not fit for further processing.

## Curve definition

The following keys are used for curve definitions:

| Key                 | Type     | Description           |
|---------------------|----------|-----------------------|
| **name**            | string   | Curve name or mnemonic. Mandatory. Non-null.              |
| **description**     | string   | Curve description. Optional.       |
| **quantity**        | string   | Curve quantity such as _length_, _pressure_, _force_ etc. Optional.|
| **unit**            | string   | Unit of measurement such as _m_, _ft_, _bar_, etc. Optional. |
| **valueType**       | string   | Curve value type: _float_, _integer_, _string_, _datetime_ or _boolean_. Non-null. Optional. _float_ assumed if not present. |
| **dimensions**      | integer  | Number of dimensions. [1,>. Non-null. Optional. 1 assumed if not present.|

Quantities and units should follow the [Unit of Measure Standard](https://www.energistics.org/energistics-unit-of-measure-standard/) from Energistics. To ease transition from legacy formats this is no requirement.

In addition to the listed, clients may add any number of _custom_ curve definition entries in any form supported by the JSON syntax.

## Curve data

Curve data are specified in arrays for each index entry, with one entry per curve. If a curve is multi-dimensional, the entry is itself an array of subentries, one per dimension.

Curve values are according to the value type defined for the curve, or `null` for no-values. The index curve is always the first curve listed, and must not contain no-values. It is advised that the index curve is continuously increasing or decreasing.

## Extended data types

To support convertion of legacy formats to JSON some standard data type extensions have been suggested.

This is the _LAS parameter_ type that will capture general LAS parameter records of the form:

```txt
<name>.<unit> <value> : <description>
```

This should be converted to JSON as follows:

```txt
"<name>": {
    "value": <value>,
    "unit": <unit>,
    "description": <description>;
}
```

For example:

```txt
   SwIrr  .V/V    0.30 : Irreducible Water Saturation
   Rshl   .OHMM   2.12 : Resistivity shale
   PDAT   .       MSL  : Permanent Datum
```

Converts to:

```json
"SwIrr": {
    "value": 0.3000,
    "unit": "V/V",
    "description": "Irreducible Water Saturation"
},
"Rshl": {
    "value": 2.12,
    "unit": "OHMM",
    "description": "Resistivity shale"
},
"PDAT": {
    "value": "MSL",
    "unit": null,
    "description": "Permanent Datum"
}
```

Another common data type for metadata is the _DLIS set_. This is a named entity with a number of attributes and a number of objects with one or more values for each of the attributes. A DLIS set has a binary representation within a DLIS file, but it can be viewed as a matrix as follows:

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

This should be converted to JSON as follows:

```txt
"setName": {
    "attributes": ["attr1", "attr2", "attr3", ... "attrn"],
    "objects": [
        "object1": [v11, v12, v13, ... v1n],
        "object1": [v21, v22, v23, ... v2n],
        "object3": [v31, v32, v33, ... v3n],
        :
        "object1": [vm1, vm2, vm3, ... vmn]
    ]
}
```

For example:

```txt
HzEquipment
          LENGTH   TRADEMARK-NAME SERIAL-NUMBER WEIGHT
---------------------------------------------------------
APWD      0.0 in   APWD25-AA      241408        0.0 kg
ARDC      224.8 in ARC9D-BA       738           1270.0 kg
MSSD900   14.5 in  SZR            FC-71545      68.0 kg
---------------------------------------------------------
```

Converts to:

```txt
"HzEquipment": {
    "attributes":
        ["LENGTH", "TRADEMARK-NAME", "SERIAL-NUMBER", "WEIGHT"],
    "objects": [
        "APWD": ["0.0 in", "APWD25-AA", "241408", "0.0 kg"],
        "ARDC": ["224.8 in", "ARC9D-BA", "738", "1270.0 kg"],
        "MSSD900": ["14.5 in", "SZR", "FC-71545", "68.0 kg"]
    ]
}
```

## Writing JSON well logs

Writing JSON well logs can be done in two different formats: _condensed_ or _pretty_. The condensed format should be without whitespace and newlines and should be used for transmission between computers only.

For well logs that may possibly be viewed by humans the pretty format should always be used. This format should contain whitespace and indentation that emphasizes the logical structure of the content. For the data section in particular, arrays of curve data for each index must be written _horizontally_ and commas between entries should be _aligned_:

```txt
"data": [
    [828420, 282.589,  8.6657, 2.202, 2.222, [1.759, 2.31469,  1.33991E-3, 3.75839], 0.52435, ... ],
    [828480, 286.239,  9.6601, 2.277, 2.297, [2.219, 2.31189,  1.12295E-3, 3.72152], 0.52387, ... ],
    [828540, 276.537, 10.6638, 2.309,  null, [2.267, 2.29509, -3.67117E-3, 3.70351], 0.53936, ... ],
    [828600, 264.325, 10.6545, 2.324,  null, [2.110, 2.27902, -7.77555E-3, 3.67927], 0.55439, ... ],
    [828660, 245.938,  9.6937, 2.333, 2.356, [1.525, 2.26512, -1.17965E-2, 3.68386], 0.56211, ... ],
    :
]
```

## More examples

* [wlc_composite.json](https://petroware.no/json/wlc_composite.json)
* [Real Time HILT - MD Log.json](https://petroware.no/json/Real%20Time%20HILT%20-%20MD%20Log.json)
* [Real Time RAB Images (LWD) - MD Log.json](https://petroware.no/json/Real%20Time%20RAB%20Images%20(LWD)%20-%20MD%20Log.json)
* [RTAC Production Data - Time Log.json](https://petroware.no/json/RTAC%20Production%20Data%20-%20Time%20Log.json)
* [SonicScope proVision - Time Log.json](https://petroware.no/json/SonicScope%20proVision%20-%20Time%20Log.json)
