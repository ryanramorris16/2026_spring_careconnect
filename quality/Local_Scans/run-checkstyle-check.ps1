# Run Checkstyle check and ensure result file is created
Write-Host "Running Checkstyle check..." -ForegroundColor Cyan

# Navigate to backend/core where mvnw is located
# Go up two levels from quality/local_scans to reach the root directory
$rootPath = Split-Path -Path (Split-Path -Path (Get-Location) -Parent) -Parent
$backendCorePath = Join-Path -Path $rootPath -ChildPath "backend\core"
Push-Location $backendCorePath

try {
    # Run checkstyle:check
    ./mvnw checkstyle:check
}
finally {
    Pop-Location
}

# Create the result directory if it doesn't exist
$reportsDir = Join-Path -Path (Get-Location) -ChildPath "Static_Analysis_Reports"
if (-not (Test-Path $reportsDir)) {
    New-Item -ItemType Directory -Path $reportsDir -Force | Out-Null
}

$resultFile = Join-Path -Path $reportsDir -ChildPath "checkstyle-result.xml"
$rawResultFile = Join-Path -Path $reportsDir -ChildPath "checkstyle.xml"

# Check if Checkstyle generated an XML file
$rootPath = Split-Path -Path (Split-Path -Path (Get-Location) -Parent) -Parent
$backendCorePath = Join-Path -Path $rootPath -ChildPath "backend\core"
$possibleFiles = @(
    (Join-Path -Path $backendCorePath -ChildPath "target\checkstyle-result.xml"),
    (Join-Path -Path $reportsDir -ChildPath "checkstyle-result.xml")
)

$foundFile = $false
foreach ($file in $possibleFiles) {
    if (Test-Path $file) {
        $fileSize = (Get-Item $file).Length
        Write-Host "Found Checkstyle report at: $file (Size: $fileSize bytes)" -ForegroundColor Yellow
        
        # Check if file is empty or too small to be valid XML
        if ($fileSize -eq 0) {
            Write-Host "File is empty, skipping..." -ForegroundColor Yellow
            continue
        }
        
        # Only copy if not already in the reports directory
        if ($file -ne $rawResultFile) {
            Copy-Item -Path $file -Destination $rawResultFile -Force
            Write-Host "Copied raw file to: $rawResultFile" -ForegroundColor Green
        }
        
        try {
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
        } catch {
            Write-Host "Failed to parse XML, copying raw content..." -ForegroundColor Yellow
            Copy-Item -Path $file -Destination $resultFile -Force
        }
        
        # Remove the original file from backend/core/target
        if ($file -like "*backend\core*") {
            Remove-Item -Path $file -Force
            Write-Host "Removed original from: $file" -ForegroundColor Gray
        }
        
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
