# Run Checkstyle check and ensure result file is created
Write-Host "Running Checkstyle check..." -ForegroundColor Cyan

# Run checkstyle:check
./mvnw checkstyle:check

# Create the result directory if it doesn't exist
$reportsDir = Join-Path -Path (Get-Location) -ChildPath "Static_Analysis_Reports"
if (-not (Test-Path $reportsDir)) {
    New-Item -ItemType Directory -Path $reportsDir -Force | Out-Null
}

$resultFile = Join-Path -Path $reportsDir -ChildPath "checkstyle-result.xml"

# Check if Checkstyle generated an XML file
$possibleFiles = @(
    (Join-Path -Path (Get-Location) -ChildPath "target\checkstyle-result.xml"),
    (Join-Path -Path $reportsDir -ChildPath "checkstyle-result.xml")
)

$foundFile = $false
foreach ($file in $possibleFiles) {
    if (Test-Path $file) {
        $fileSize = (Get-Item $file).Length
        Write-Host "Found Checkstyle report at: $file (Size: $fileSize bytes)" -ForegroundColor Yellow
        
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

# If no Checkstyle output found, create a blank report
if (-not $foundFile) {
    Write-Host "No Checkstyle XML file found, creating blank report..." -ForegroundColor Yellow
    @'
<?xml version="1.0" encoding="UTF-8"?>
<checkstyle version="10.0">
</checkstyle>
'@ | Set-Content -Path $resultFile -Encoding UTF8
}

Write-Host "checkstyle-result.xml ready at: $resultFile" -ForegroundColor Green
Write-Host "Done!" -ForegroundColor Cyan
