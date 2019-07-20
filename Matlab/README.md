# Accessing JSON Well Log Format files with MATLAB

As MATLAB has built-in JSON support, accessing JSON Well Log Format files with
MATLAB is trivial. Loading and parsing the logs of a .json file is done as follows:

```MATLAB
filename = "path/to/file.json";
logs = jsondecode(fileread(filename));
header = logs.header;
curves = logs.curves;
data = logs.data;
```

For the ```demo.json``` file available in this repository the result is as follows:

![Matrix](https://jsonwelllogformat.org/images/matlabMatrix.png)



## More test data

The public _Volve_ data from Equinor is available in the JSON Well Log Format
at [https://jsonwelllogformat.org/Volve](https://jsonwelllogformat.org/Volve).

