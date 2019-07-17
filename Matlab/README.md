# Read JSON Well Log with Matlab

## Example code

```
fname = "path/to/file.json";
json = jsondecode(fileread(fname));
header = json.header;
curves = json.curves;
data = json.data;
```