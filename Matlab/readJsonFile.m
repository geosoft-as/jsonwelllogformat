filename = "path/to/file.json";
json = jsondecode(fileread(filename));
header = json.header;
curves = json.curves;
data = json.data;
