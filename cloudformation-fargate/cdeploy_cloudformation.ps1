param(
    [ValidateSet("dev", "cfdemo", "staging", "prod")]
    [string]$Environment = "dev",

    [string]$Profile = "careconnect-sso",

    [string]$Region = "us-east-1",

    [string]$ImageTag,

    [switch]$RunTests
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$script:StartTime = Get-Date

# Track the active stack/operation so the catch block can print useful context.
$script:CurrentStackName = $null
$script:HadNativePreference = $false
$nativePreferenceVar = Get-Variable -Name PSNativeCommandUseErrorActionPreference -Scope Global -ErrorAction SilentlyContinue
if ($null -ne $nativePreferenceVar) {
    $script:HadNativePreference = $true
    $script:OriginalNativePreference = $nativePreferenceVar.Value
    $global:PSNativeCommandUseErrorActionPreference = $false
}

if (-not $ImageTag) {
    # Default the image tag to the environment name so dev/cfdemo stay separate.
    $ImageTag = $Environment
}

# Resolve repository-relative paths so the script works from any starting folder.
$ScriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = Split-Path -Parent $ScriptRoot
$TemplateDir = Join-Path $ScriptRoot "templates"
$ParameterDir = Join-Path $ScriptRoot "parameters"
$BackendDir = Join-Path $RepoRoot "backend\core"

$StackPrefix = "careconnect"
$NetworkingStackName = "$StackPrefix-networking-$Environment"
$DataStackName = "$StackPrefix-data-$Environment"
$PlatformStackName = "$StackPrefix-platform-$Environment"
$ServiceStackName = "$StackPrefix-service-$Environment"

$NetworkingTemplate = Join-Path $TemplateDir "01-networking.yaml"
$DataTemplate = Join-Path $TemplateDir "02-data.yaml"
$PlatformTemplate = Join-Path $TemplateDir "03-platform.yaml"
$ServiceTemplate = Join-Path $TemplateDir "04-service.yaml"

$NetworkingParameters = Join-Path $ParameterDir "$Environment-networking.json"
$DataParameters = Join-Path $ParameterDir "$Environment-data.json"
$PlatformParameters = Join-Path $ParameterDir "$Environment-platform.json"
$ServiceParameters = Join-Path $ParameterDir "$Environment-service.json"

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Test-CommandExists {
    param([string]$Name)
    return [bool](Get-Command $Name -ErrorAction SilentlyContinue)
}

function Assert-LastExitCode {
    param([string]$Context)
    if ($LASTEXITCODE -ne 0) {
        throw "$Context failed with exit code $LASTEXITCODE."
    }
}

function Get-ElapsedTimeText {
    $elapsed = (Get-Date) - $script:StartTime
    return "{0:00}:{1:00}:{2:00}" -f [int]$elapsed.TotalHours, $elapsed.Minutes, $elapsed.Seconds
}

function Test-StackExists {
    param([string]$StackName)

    & aws cloudformation describe-stacks `
        --profile $Profile `
        --region $Region `
        --stack-name $StackName 2>$null 1>$null

    return ($LASTEXITCODE -eq 0)
}

function Get-StackStatus {
    param([string]$StackName)

    $status = & aws cloudformation describe-stacks `
        --profile $Profile `
        --region $Region `
        --stack-name $StackName `
        --query "Stacks[0].StackStatus" `
        --output text 2>$null

    if ($LASTEXITCODE -ne 0) {
        return $null
    }

    return [string]$status
}

function Remove-RollbackCompleteStack {
    param([string]$StackName)

    $stackStatus = Get-StackStatus -StackName $StackName
    if ($stackStatus -ne "ROLLBACK_COMPLETE") {
        return
    }

    Write-Host "Stack '$StackName' is in ROLLBACK_COMPLETE. Deleting it before retrying deployment..." -ForegroundColor Yellow
    & aws cloudformation delete-stack `
        --profile $Profile `
        --region $Region `
        --stack-name $StackName
    Assert-LastExitCode "CloudFormation delete-stack for rollback recovery on '$StackName'"

    & aws cloudformation wait stack-delete-complete `
        --profile $Profile `
        --region $Region `
        --stack-name $StackName
    Assert-LastExitCode "CloudFormation wait stack-delete-complete for rollback recovery on '$StackName'"
}

function Write-StackFailureDetails {
    param([string]$StackName)

    $stackStatus = Get-StackStatus -StackName $StackName
    if ($stackStatus) {
        Write-Host ""
        Write-Host "Stack status: $stackStatus" -ForegroundColor Yellow
    }

    Write-Host "Recent failed CloudFormation events for '$StackName':" -ForegroundColor Yellow
    & aws cloudformation describe-stack-events `
        --profile $Profile `
        --region $Region `
        --stack-name $StackName `
        --query "StackEvents[?contains(ResourceStatus, 'FAILED')].[Timestamp,LogicalResourceId,ResourceType,ResourceStatus,ResourceStatusReason]" `
        --output table
}

function Assert-PathExists {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Required path not found: $Path"
    }
}

function Get-ParameterOverrides {
    param(
        [string]$ParameterFile,
        [hashtable]$Overrides = @{}
    )

    # Read the checked-in JSON parameter file and convert it into the
    # Key=Value format expected by `aws cloudformation deploy`.
    $entries = Get-Content -LiteralPath $ParameterFile -Raw | ConvertFrom-Json
    $result = New-Object System.Collections.Generic.List[string]

    foreach ($entry in $entries) {
        $key = [string]$entry.ParameterKey
        $value = [string]$entry.ParameterValue

        if ($Overrides.ContainsKey($key)) {
            $value = [string]$Overrides[$key]
        }

        $result.Add("$key=$value")
    }

    return $result.ToArray()
}

function Test-PlaceholderValue {
    param(
        [string]$ParameterFile,
        [string[]]$DisallowedFragments
    )

    $entries = Get-Content -LiteralPath $ParameterFile -Raw | ConvertFrom-Json
    foreach ($entry in $entries) {
        $value = [string]$entry.ParameterValue
        foreach ($fragment in $DisallowedFragments) {
            if ($value -like "*$fragment*") {
                throw "Parameter file '$ParameterFile' still contains a placeholder value for '$($entry.ParameterKey)'."
            }
        }
    }
}

function Invoke-CloudFormationDeploy {
    param(
        [string]$StackName,
        [string]$TemplatePath,
        [string]$ParameterFile,
        [hashtable]$Overrides = @{}
    )

    # CloudFormation deploy handles both create and update. We still detect the
    # current state so the user can see which path is happening.
    $script:CurrentStackName = $StackName
    Remove-RollbackCompleteStack -StackName $StackName
    $parameterOverrides = Get-ParameterOverrides -ParameterFile $ParameterFile -Overrides $Overrides
    $operation = if (Test-StackExists -StackName $StackName) { "Updating" } else { "Creating" }
    Write-Host "$operation stack '$StackName'..." -ForegroundColor DarkCyan

    $args = @(
        "cloudformation", "deploy",
        "--profile", $Profile,
        "--region", $Region,
        "--stack-name", $StackName,
        "--template-file", $TemplatePath,
        "--capabilities", "CAPABILITY_NAMED_IAM",
        "--no-fail-on-empty-changeset",
        "--parameter-overrides"
    ) + $parameterOverrides

    & aws @args
    if ($LASTEXITCODE -ne 0) {
        Write-StackFailureDetails -StackName $StackName
        throw "CloudFormation deploy for stack '$StackName' failed with exit code $LASTEXITCODE."
    }

    $finalStatus = Get-StackStatus -StackName $StackName
    if ($finalStatus) {
        Write-Host "Stack '$StackName' is now $finalStatus." -ForegroundColor Green
    }
}

function Get-CloudFormationOutput {
    param(
        [string]$StackName,
        [string]$OutputKey
    )

    $value = & aws cloudformation describe-stacks `
        --profile $Profile `
        --region $Region `
        --stack-name $StackName `
        --query "Stacks[0].Outputs[?OutputKey=='$OutputKey'].OutputValue" `
        --output text
    Assert-LastExitCode "Reading CloudFormation output '$OutputKey' from stack '$StackName'"
    return [string]$value
}

try {
Write-Step "Checking prerequisites"
    foreach ($command in @("aws", "docker", "git", "java")) {
        if (-not (Test-CommandExists $command)) {
            throw "Required command not found in PATH: $command"
        }
    }

    Assert-PathExists $NetworkingTemplate
    Assert-PathExists $DataTemplate
    Assert-PathExists $PlatformTemplate
    Assert-PathExists $ServiceTemplate
    Assert-PathExists $NetworkingParameters
    Assert-PathExists $DataParameters
    Assert-PathExists $PlatformParameters
    Assert-PathExists $ServiceParameters
    Assert-PathExists $BackendDir

    if (-not (Test-Path -LiteralPath (Join-Path $BackendDir "mvnw.cmd"))) {
        throw "Expected Maven wrapper not found at '$BackendDir\\mvnw.cmd'."
    }

    # Fail fast if secrets/config placeholders were never replaced.
    Test-PlaceholderValue -ParameterFile $DataParameters -DisallowedFragments @("REPLACE_ME")

    Write-Step "Verifying AWS credentials for profile '$Profile'"
    & aws sts get-caller-identity --profile $Profile --region $Region | Out-Null
    Assert-LastExitCode "AWS credential validation"

    # Stack order matters: networking -> data -> platform -> image push -> service.
    # Later stacks import values created by earlier ones.
    Write-Step "Deploying networking stack: $NetworkingStackName"
    Invoke-CloudFormationDeploy -StackName $NetworkingStackName -TemplatePath $NetworkingTemplate -ParameterFile $NetworkingParameters

    Write-Step "Deploying data stack: $DataStackName"
    Invoke-CloudFormationDeploy -StackName $DataStackName -TemplatePath $DataTemplate -ParameterFile $DataParameters

    Write-Step "Deploying platform stack: $PlatformStackName"
    Invoke-CloudFormationDeploy -StackName $PlatformStackName -TemplatePath $PlatformTemplate -ParameterFile $PlatformParameters

    # The platform stack creates the ECR repository. We need that URI before the
    # Docker image can be tagged and pushed.
    Write-Step "Reading ECR repository URI"
    $RepositoryUri = (Get-CloudFormationOutput -StackName $PlatformStackName -OutputKey "EcrRepositoryUri").Trim()
    if (-not $RepositoryUri) {
        throw "Platform stack did not return EcrRepositoryUri."
    }

    $RepositoryName = ($RepositoryUri -split "/", 2)[1]
    $ImageUri = "$RepositoryUri`:$ImageTag"
    $LocalImageName = "careconnect-backend-local:$ImageTag"

    # Package the backend as the Dockerfile expects: Spring Boot fat jar using
    # the docker Maven profile.
    Write-Step "Building backend jar"
    Push-Location $BackendDir
    try {
        $mavenArgs = @("clean", "package", "-Pdocker")
        if (-not $RunTests) {
            $mavenArgs += "-DskipTests"
        }
        & .\mvnw.cmd @mavenArgs
        Assert-LastExitCode "Maven package build"

        # Authenticate Docker to the registry before push.
        Write-Step "Logging into ECR"
        $RegistryHost = ($RepositoryUri -split "/", 2)[0]
        $LoginPassword = & aws ecr get-login-password --profile $Profile --region $Region
        Assert-LastExitCode "ECR login password retrieval"
        $LoginPassword | docker login --username AWS --password-stdin $RegistryHost
        Assert-LastExitCode "Docker login to ECR"

        Write-Step "Building Docker image"
        & docker build -t $LocalImageName .
        Assert-LastExitCode "Docker build"

        Write-Step "Tagging and pushing Docker image to ECR"
        & docker tag $LocalImageName $ImageUri
        Assert-LastExitCode "Docker tag"
        & docker push $ImageUri
        Assert-LastExitCode "Docker push"
    }
    finally {
        Pop-Location
    }

    # The service stack is deployed last because it needs the final image URI.
    Write-Step "Deploying service stack: $ServiceStackName"
    $ServiceOverrides = @{
        BackendImageUri = $ImageUri
    }
    Invoke-CloudFormationDeploy -StackName $ServiceStackName -TemplatePath $ServiceTemplate -ParameterFile $ServiceParameters -Overrides $ServiceOverrides

    # Print the final ALB endpoint so the frontend or health checks can use it.
    Write-Step "Reading final backend URL"
    $AlbDnsName = (Get-CloudFormationOutput -StackName $ServiceStackName -OutputKey "LoadBalancerDnsName").Trim()
    $AlbUrl = (Get-CloudFormationOutput -StackName $ServiceStackName -OutputKey "LoadBalancerUrl").Trim()

    Write-Host ""
    Write-Host "Deployment complete." -ForegroundColor Green
    Write-Host "Environment:   $Environment"
    Write-Host "Repository:    $RepositoryName"
    Write-Host "Image URI:     $ImageUri"
    Write-Host "ALB DNS:       $AlbDnsName"
    Write-Host "Backend URL:   $AlbUrl"
    Write-Host "Health check:  $AlbUrl/v1/api/test/health"
    Write-Host "Elapsed time:  $(Get-ElapsedTimeText)"
}
catch {
    Write-Host ""
    Write-Host "Deployment failed." -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    Write-Host "Elapsed time: $(Get-ElapsedTimeText)" -ForegroundColor Yellow

    if ($script:CurrentStackName) {
        Write-Host ""
        Write-Host "Troubleshoot this stack with:" -ForegroundColor Yellow
        Write-Host "aws cloudformation describe-stack-events --profile $Profile --region $Region --stack-name $script:CurrentStackName --query `"StackEvents[?contains(ResourceStatus, 'FAILED')].[Timestamp,LogicalResourceId,ResourceType,ResourceStatus,ResourceStatusReason]`" --output table" -ForegroundColor Yellow
    }

    exit 1
}
finally {
    if ($script:HadNativePreference) {
        $global:PSNativeCommandUseErrorActionPreference = $script:OriginalNativePreference
    }
}
