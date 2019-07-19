# Accessing JSON Well Log Format files with Python

As Python has built-in JSON support, accessing JSON Well Log Format files with
Python is trivial. Loading and parsing the logs of a .json file is done as follows:

```python
import json

with open("<filename>", "r") as file:
  logs = json.load(file)
```

The ```logs``` instance now holds the content of the file in a structural form,
where the header data, curve entries and the log data can be easily be accessed as:

```python
header = logs[0]['header']
curves = logs[0]['curves']
data = logs[0]['data']
```

etc.


## Example

The following example demonstrates how easy it is to parse a JSON Well Log Format
file and create a basic log plot. The program utilize the
_matplotlib_ plotting library from [matplotlib.org](https://matplotlib.org).


```python
import matplotlib.pyplot as plot
import json

with open("demo.json", "r") as file:
  logs = json.load(file)

curves = logs[0]['curves']
data = logs[0]['data']

index = list(map(lambda row: row[0], data))

for curveNo in range(1, len(curves)):
  curve = list(map(lambda row: row[curveNo], data))
  plot.subplot(1, len(curves), curveNo)
  plot.plot(curve, index)

  axis = plot.gca()
    axis.invert_yaxis()
    axis.xaxis.tick_top()
    axis.xaxis.set_label_position('top')
    if curveNo > 1: axis.set_yticklabels([])

    if curveNo == 1:
      plot.ylabel(f'{curves[0]["name"]} [{curves[0]["unit"]}]')
    plot.xlabel(f'{curves[curveNo]["name"]} [{curves[curveNo]["unit"]}]')

plot.gcf().suptitle("JSON Well Log Format")
plot.show()
```


To install dependencies and run the program:

```
$ pip install --user --requirement requirements.txt
$ python plotlog.py
```


For the ```demo.json``` file available in this repository the result is as follows:


![Plot](https://petroware.no/images/pythonPlot.png)



## More test data

The public _Volve_ data from Equinor is available in the JSON Well Log Format
at [https://jsonwelllogformat.org/Volve](https://jsonwelllogformat.org/Volve).

