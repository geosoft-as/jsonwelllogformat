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


## Data types


## Logging metadata

## Curve definition


## Extended data types

## Writing JSON well logs

## More examples
