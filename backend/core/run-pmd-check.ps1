# Run PMD check and ensure result files are created
Write-Host "Running PMD check..." -ForegroundColor Cyan

# Run PMD checks (PMD + CPD)
./mvnw pmd:check pmd:cpd-check
if ($LASTEXITCODE -ne 0) {
    Write-Host "PMD reported violations or errors (exit code $LASTEXITCODE). Continuing to collect reports..." -ForegroundColor Yellow
}

# Create the result directory if it doesn't exist
$reportsDir = Join-Path -Path (Get-Location) -ChildPath "Static_Analysis_Reports"
if (-not (Test-Path $reportsDir)) {
    New-Item -ItemType Directory -Path $reportsDir -Force | Out-Null
}

$pmdResultFile = Join-Path -Path $reportsDir -ChildPath "pmd-result.xml"
$cpdResultFile = Join-Path -Path $reportsDir -ChildPath "pmd-cpd-result.xml"

# Ensure result files are overwritten
if (Test-Path $pmdResultFile) {
    Remove-Item -Path $pmdResultFile -Force
}
if (Test-Path $cpdResultFile) {
    Remove-Item -Path $cpdResultFile -Force
}

function Write-FormattedXmlFile {
    param (
        [Parameter(Mandatory = $true)]
        [string]$SourcePath,
        [Parameter(Mandatory = $true)]
        [string]$DestinationPath
    )

    try {
        [xml]$xmlContent = Get-Content -Path $SourcePath
        $xmlSettings = New-Object System.Xml.XmlWriterSettings
        $xmlSettings.Indent = $true
        $xmlSettings.IndentChars = "  "
        $xmlSettings.Encoding = New-Object System.Text.UTF8Encoding($false)

        $stringWriter = New-Object System.IO.StringWriter
        $xmlWriter = [System.Xml.XmlWriter]::Create($stringWriter, $xmlSettings)
        $xmlContent.WriteTo($xmlWriter)
        $xmlWriter.Flush()
        $xmlWriter.Close()

        $stringWriter.ToString() | Set-Content -Path $DestinationPath -Encoding UTF8
    } catch {
        Write-Host "XML formatting failed for $SourcePath. Copying raw content..." -ForegroundColor Yellow
        Get-Content -Path $SourcePath -Raw | Set-Content -Path $DestinationPath -Encoding UTF8
    }
}

function Write-XmlDocument {
    param (
        [Parameter(Mandatory = $true)]
        [xml]$XmlDocument,
        [Parameter(Mandatory = $true)]
        [string]$DestinationPath
    )

    $xmlSettings = New-Object System.Xml.XmlWriterSettings
    $xmlSettings.Indent = $true
    $xmlSettings.IndentChars = "  "
    $xmlSettings.Encoding = New-Object System.Text.UTF8Encoding($false)

    $stringWriter = New-Object System.IO.StringWriter
    $xmlWriter = [System.Xml.XmlWriter]::Create($stringWriter, $xmlSettings)
    $XmlDocument.WriteTo($xmlWriter)
    $xmlWriter.Flush()
    $xmlWriter.Close()

    $stringWriter.ToString() | Set-Content -Path $DestinationPath -Encoding UTF8
}

function Update-PmdViolationCount {
    param (
        [Parameter(Mandatory = $true)]
        [string]$ReportPath
    )

    try {
        [xml]$xmlContent = Get-Content -Path $ReportPath
        $namespaceUri = $xmlContent.DocumentElement.NamespaceURI

        if ([string]::IsNullOrWhiteSpace($namespaceUri)) {
            $violationCount = ($xmlContent.SelectNodes("//violation")).Count
        } else {
            $nsManager = New-Object System.Xml.XmlNamespaceManager($xmlContent.NameTable)
            $nsManager.AddNamespace("pmd", $namespaceUri)
            $violationCount = ($xmlContent.SelectNodes("//pmd:violation", $nsManager)).Count
        }

        $xmlContent.DocumentElement.SetAttribute("violationCount", $violationCount)
        Write-XmlDocument -XmlDocument $xmlContent -DestinationPath $ReportPath
    } catch {
        Write-Host "Failed to update PMD violation count for $ReportPath" -ForegroundColor Yellow
    }
}

function Update-CpdViolationCount {
    param (
        [Parameter(Mandatory = $true)]
        [string]$ReportPath
    )

    try {
        [xml]$xmlContent = Get-Content -Path $ReportPath
        $duplicationCount = ($xmlContent.SelectNodes("//duplication")).Count
        $xmlContent.DocumentElement.SetAttribute("violationCount", $duplicationCount)
        Write-XmlDocument -XmlDocument $xmlContent -DestinationPath $ReportPath
    } catch {
        Write-Host "Failed to update CPD duplication count for $ReportPath" -ForegroundColor Yellow
    }
}

# Check if PMD generated an XML file
$possiblePmdFiles = @(
    (Join-Path -Path (Get-Location) -ChildPath "target\pmd.xml"),
    (Join-Path -Path (Get-Location) -ChildPath "target\pmd-result.xml"),
    (Join-Path -Path $reportsDir -ChildPath "pmd.xml"),
    (Join-Path -Path $reportsDir -ChildPath "pmd-result.xml")
)

$foundPmdFile = $false
foreach ($file in $possiblePmdFiles) {
    if (Test-Path $file) {
        $fileSize = (Get-Item $file).Length
        Write-Host "Found PMD report at: $file (Size: $fileSize bytes)" -ForegroundColor Yellow

        Write-FormattedXmlFile -SourcePath $file -DestinationPath $pmdResultFile
        Update-PmdViolationCount -ReportPath $pmdResultFile
        Write-Host "Copied and formatted to: $pmdResultFile" -ForegroundColor Green
        $foundPmdFile = $true
        break
    }
}

# If no PMD output found, create a blank report
if (-not $foundPmdFile) {
    Write-Host "No PMD XML file found, creating blank report..." -ForegroundColor Yellow
    @'
<?xml version="1.0" encoding="UTF-8"?>
<pmd>
</pmd>
'@ | Set-Content -Path $pmdResultFile -Encoding UTF8
    Update-PmdViolationCount -ReportPath $pmdResultFile
}

# Check if CPD generated an XML file
$possibleCpdFiles = @(
    (Join-Path -Path (Get-Location) -ChildPath "target\pmd-cpd.xml"),
    (Join-Path -Path (Get-Location) -ChildPath "target\cpd.xml"),
    (Join-Path -Path (Get-Location) -ChildPath "target\pmd-cpd-result.xml"),
    (Join-Path -Path $reportsDir -ChildPath "pmd-cpd.xml"),
    (Join-Path -Path $reportsDir -ChildPath "cpd.xml"),
    (Join-Path -Path $reportsDir -ChildPath "pmd-cpd-result.xml")
)

$foundCpdFile = $false
foreach ($file in $possibleCpdFiles) {
    if (Test-Path $file) {
        $fileSize = (Get-Item $file).Length
        Write-Host "Found PMD CPD report at: $file (Size: $fileSize bytes)" -ForegroundColor Yellow

        Write-FormattedXmlFile -SourcePath $file -DestinationPath $cpdResultFile
        Update-CpdViolationCount -ReportPath $cpdResultFile
        Write-Host "Copied and formatted to: $cpdResultFile" -ForegroundColor Green
        $foundCpdFile = $true
        break
    }
}

# If no CPD output found, create a blank report
if (-not $foundCpdFile) {
    Write-Host "No PMD CPD XML file found, creating blank report..." -ForegroundColor Yellow
    @'
<?xml version="1.0" encoding="UTF-8"?>
<pmd-cpd>
</pmd-cpd>
'@ | Set-Content -Path $cpdResultFile -Encoding UTF8
    Update-CpdViolationCount -ReportPath $cpdResultFile
}

Write-Host "pmd-result.xml ready at: $pmdResultFile" -ForegroundColor Green
Write-Host "pmd-cpd-result.xml ready at: $cpdResultFile" -ForegroundColor Green
Write-Host "Done!" -ForegroundColor Cyan
