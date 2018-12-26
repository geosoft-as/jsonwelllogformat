# Loading JSON Well Log files to MS/Excel

With the simple macro available here, files in the JSON Well Log Format can
be loaded into Microsoft Excel in two simple steps:

<img hspace="100" width="700" src="https://jsonwelllogformat.org/images/excel1.png">


## Installation

Install the JSON Well Log Format reader macro by following the instructions below:

1. Open Microsoft Excel

These few steps are done in order to save the macro definition across
worksheets so that the function will always be available:

2. Select the "Developer" ribbon

3. Select "Record Macro"

4. In the "Record Macro" dialog just keep the default name, for "Store macro in:", select
   "Personal Macro Workbook" and click "OK".

Now we will define the Macro itself:

2. From the "Developer" ribbon, select "Visual Basic" to open the "Visual Basic for Applications" window

3. Select Tools->References... and tick "Microsoft Scripting Runtime"

4. Make sure the project explorer is visible by selecting View->Project Explorer

2. Expand the VBAProject (PERSONAL.XLSB)

4. Right click the Module1 entry and select Import File...

5. Select the JsonConverter.bas file and click Open

4. Right click the Module1 entry and select Import File...

6. Select the JsonWellLogFormatReader.bas file and click Open

7. File -> Save PERSONAL.XLSB

7. Close the Visual Basic for Application window

Now the JSON Well Log Format macro is installed and can be used.
But we will improve the usability by putting it directly on a ribbon.

1. Right click the ribbon and select "Customize the Ribbon..." to open "Excel Options"

2. In the "Choose commands from", select "Macros"

3. The function can be put in any ribbon, we will choose the Data ribbon.

3. Select the "Get External Data" group and click the "New Group" button at the bottom

5. Click "Rename..." to give the new group a descriptive name, for instance "JSON"

6. Select the "PERSONAL.XLSB!LoadJsonWellLogFormat" macro on the left and click "Add >>>"

7. Select the "PERSONAL.XLSB!LoadJsonWellLogFormat" entry on the right and click "Rename..."

8. Give it a descriptive name like "Load Well Log" and choose an icon

9. Click "OK" to close the window

The macro is now installed, and will be available from the Data ribbon


## Usage

1. Start Microsoft Excel

2. Select the Data ribbon

3. Click the "Load Well Log" button











3. Select the LoadJsonWellLogFormat macro from the list




In order to read JSON files in Excel, the JsonWellLogFormatReader macro
needs to be