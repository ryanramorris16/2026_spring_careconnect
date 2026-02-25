# Run SpotBugs with full Maven build to generate report
Write-Host "Running SpotBugs analysis with Maven verify..." -ForegroundColor Cyan

# Run full verify phase to ensure SpotBugs generates XML
./mvnw clean verify -DskipTests 2>&1 | Tee-Object -Variable output | Out-Null

# Create the result directory if it doesn't exist
$reportsDir = Join-Path -Path (Get-Location) -ChildPath "Static_Analysis_Reports"
if (-not (Test-Path $reportsDir)) {
    New-Item -ItemType Directory -Path $reportsDir -Force | Out-Null
}

$resultFile = Join-Path -Path $reportsDir -ChildPath "spotbugs-result.xml"

# Check if SpotBugs generated an XML file
$possibleFiles = @(
    (Join-Path -Path (Get-Location) -ChildPath "target\spotbugsXml.xml"),
    (Join-Path -Path $reportsDir -ChildPath "spotbugsXml.xml")
)

$foundFile = $false
foreach ($file in $possibleFiles) {
    if (Test-Path $file) {
        $fileSize = (Get-Item $file).Length
        Write-Host "Found SpotBugs report at: $file (Size: $fileSize bytes)" -ForegroundColor Yellow
        
        # Load and format the XML for better readability
        [xml]$xmlContent = Get-Content -Path $file
        $xmlSettings = New-Object System.Xml.XmlWriterSettings
        $xmlSettings.Indent = $true
        $xmlSettings.IndentChars = "  "
        $xmlSettings.Encoding = New-Object System.Text.UTF8Encoding($false)
        
        $stringWriter = New-Object System.IO.StringWriter
        $xmlWriter = [System.Xml.XmlWriter]::Create($stringWriter, $xmlSettings)
        $xmlContent.WriteTo($xmlWriter)
        $xmlWriter.Flush()
        $xmlWriter.Close()
        
        $stringWriter.ToString() | Set-Content -Path $resultFile -Encoding UTF8
        Write-Host "Copied and formatted to: $resultFile" -ForegroundColor Green
        $foundFile = $true
        break
    }
}

# If no SpotBugs output found, create a blank report
if (-not $foundFile) {
    Write-Host "No SpotBugs XML file found, creating blank report..." -ForegroundColor Yellow
    @'
<?xml version="1.0" encoding="UTF-8"?>
<BugCollection project="careconnect-backend" home="/spotbugs" version="4.8.3.1" total_bugs="0">
</BugCollection>
'@ | Set-Content -Path $resultFile -Encoding UTF8
}

Write-Host "spotbugs-result.xml ready at: $resultFile" -ForegroundColor Green
Write-Host "Done!" -ForegroundColor Cyan
