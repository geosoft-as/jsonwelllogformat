# Open JSON Well Log files in MS/Excel

With the simple macro available here, files in the JSON Well Log Format can
be opened in Microsoft Excel in two simple steps:

<img hspace="20" width="1000" src="https://jsonwelllogformat.org/images/excel3.png">


## Installation

Installing the JSON macros and adding the function to the Excel ribbon can
be done by the following the simple procedure:

1. Save [JsonConverter.bas](JsonConverter.bas) and [JsonWellLogFormatReader.bas](JsonWellLogFormatReader.bas) to local disk

2. Open Microsoft Excel

The following steps are necessary to make the macro available _across worksheets_:

3. Select the **Developer** ribbon

4. Click the **Record Macro** button to open the _Record Macro_ dialog

5. For _Store macro in:_ select **Personal Macro Workbook** and click **OK**

Now we import the macro itself:

6. From the _Developer_ ribbon, select **Visual Basic** to open the _Visual Basic for Applications_ window

7. Select **Tools->References...** and tick **Microsoft Scripting Runtime**

8. Make sure the project explorer is visible by selecting **View->Project Explorer**

9. Expand the **VBAProject (PERSONAL.XLSB)** node in the Project Explorer

10. Right click the **Module1** entry and select **Import File...**

11. Select the **JsonConverter.bas** file and click **Open**

12. Right click the **Module1** entry a second time and select **Import File...**

13. Select the **JsonWellLogFormatReader.bas** file and click **Open**

14. Select **File->Save PERSONAL.XLSB** to save it all

15. **Close** the Visual Basic for Application window

Now the JSON Well Log Format macro is installed and can be used, but we will improve
the usability by putting it directly on a ribbon.

16. Right click the ribbon and select **Customize the Ribbon...** to open _Excel Options_

17. In the _Choose commands from_, select **Macros**

18. The function can be put in any ribbon, we will choose the _Data_ ribbon.

19. Select the **Get External Data** group and click the **New Group** button at the bottom

20. Click **Rename...** to give the new group a descriptive name, for instance "JSON"

21. Select the **PERSONAL.XLSB!LoadJsonWellLogFormat** macro on the left and click **Add >>>**

4. Select the **PERSONAL.XLSB!LoadJsonWellLogFormat** entry on the right and click **Rename...**

4. Give it a descriptive name like **Open Well Log** and choose an icon

4. Click **OK** to close the window

The macro is now installed and is available from the _Data_ ribbon as shown in the
initial image.