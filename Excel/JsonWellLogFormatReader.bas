Attribute VB_Name = "JsonWellLogFormatReader"
Public Sub LoadJsonWellLogFormat()

  '
  ' Open file dialog and ask user for a JSON file
  '
  Dim fileDialog As Office.fileDialog
  Set fileDialog = Application.fileDialog(msoFileDialogFilePicker)
  fileDialog.AllowMultiSelect = False
  fileDialog.Title = "Select JSON Well Log Format file"
  fileDialog.Filters.Add "JSON", "*.json"
  fileDialog.Filters.Add "All", "*.*"

  '
  ' Quit here is user push Cancel
  '
  If fileDialog.Show = False Then
    Exit Sub
  End If

  '
  ' Capture the user selected file and read as text
  '
  fileName = fileDialog.SelectedItems(1)

  Dim FileSystemObject As New FileSystemObject
  Dim jsonTextStream As TextStream
  Set jsonTextStream = FileSystemObject.OpenTextFile(fileName, ForReading)
  jsonText = jsonTextStream.ReadAll
  jsonTextStream.Close

  '
  ' Parse the text string into a JSON object
  '
  Set logs = ParseJson(jsonText)

  '
  ' Loop over all logs and put each in a separate sheet
  '
  sheetNo = 1
  For Each logObject In logs

    ' Clear sheet content
    Sheets(sheetNo).Cells.Delete

    ' Use log name as the sheet name
    logName = "Log"
    If Not IsNull(logObject.Item("header")) And Not IsNull(logObject.Item("header")("name")) Then
      logName = logObject.Item("header")("name")
    End If

    Sheets(sheetNo).Name = logName

    ' Loop over the curve definitions and populate curve name
    ' (row 1) and unit (row 2). If multi-dimensional append _n
    ' to the curve name
    Set curveDefinitions = logObject.Item("curves")

    columnNo = 1
    For Each curveDefinition In curveDefinitions
      nDimensions = curveDefinition.Item("dimensions")
      If IsNull(nDimensions) Then
        nDimensions = 1
      End If
      curveName = curveDefinition.Item("name")
      unit = curveDefinition.Item("unit")

      columnName = curveName

      For dimension = 1 To nDimensions
        If nDimensions > 1 Then
          columnName = curveName & "_" & dimension
        End If

        Sheets(sheetNo).Cells(1, columnNo).Value = columnName
        Sheets(sheetNo).Cells(2, columnNo).Value = unit

        columnNo = columnNo + 1
      Next dimension
    Next

    ' Loop over the data rows and populate sheet rows accordingly
    Set dataRows = logObject.Item("data")
    rowNo = 3
    For Each dataRow In dataRows
       columnNo = 1
       For Each curveValue In dataRow
         If TypeOf curveValue Is Collection Then
           For Each curveSubValue In curveValue
             Sheets(sheetNo).Cells(rowNo, columnNo).Value = curveSubValue
             columnNo = columnNo + 1
           Next
         Else
           Sheets(sheetNo).Cells(rowNo, columnNo).Value = curveValue
           columnNo = columnNo + 1
         End If
       Next
       rowNo = rowNo + 1
    Next

    sheetNo = sheetNo + 1
  Next
End Sub
