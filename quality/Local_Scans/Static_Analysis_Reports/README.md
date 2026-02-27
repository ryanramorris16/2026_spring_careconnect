# Static Analysis Tools Integration

This project integrates static analysis tools into both the local development environment (via VS Code extensions) and the Maven build pipeline to improve code quality and security.

## Tools Included

### 1. **Checkstyle** 
- Enforces coding standards and style conventions
- Uses Google Checks configuration
- Runs during the `validate` phase
- Fails the build on violations

### 2. **SpotBugs**
- Detects common bug patterns in Java code
- Configured to maximum effort and low threshold
- Runs during the `verify` phase
- Flags potential bugs before runtime

### 3. **PMD (Programming Mistake Detector)**
- Identifies common coding mistakes and suboptimal patterns
- Includes copy-paste detector (CPD) for code duplication
- Runs during the `verify` phase
- Uses Java best practices ruleset

### 4. **SonarQube** (IDE + CI/CD)
- **SonarQube for IDE**: VS Code extension for real-time local analysis
- **CI/CD Integration**: Comprehensive quality tracking in the pipeline
- Tracks technical debt and code coverage
- Quality gate validation and security hotspot detection

### 5. **OWASP Dependency-Check**
- Scans dependencies for known vulnerabilities (CVEs)
- Identifies security risks in third-party libraries
- Fails build if Critical/High severity vulnerabilities found
- Generates detailed security reports

## SonarQube for IDE Setup (VS Code)

The **SonarQube for IDE** extension enables real-time code analysis in VS Code before you commit.

### Installation

1. Open VS Code
2. Go to Extensions (Ctrl+Shift+X / Cmd+Shift+X)
3. Search for "SonarQube for IDE"
4. Install the official SonarQube/SonarCloud extension

### Connected Mode Configuration

To analyze code against your SonarQube Server or SonarCloud:

1. Open Command Palette (Ctrl+Shift+P / Cmd+Shift+P)
2. Run: `SonarQube: Configure Connection`
3. Choose your connection type:
   - **SonarQube Server**: Enter server URL and authentication token
   - **SonarCloud**: Select your organization key

4. Once connected, the extension will:
   - Show issues inline in the editor
   - Provide quality metrics in the SonarQube panel
   - Sync rulesets from your SonarQube configuration
   - Highlight security hotspots and bugs in real-time

### Local Testing (Without Server)

If you don't have a SonarQube server, the extension can still show local analysis:

1. The extension will use default rulesets
2. Install the extension and it will provide real-time feedback
3. To enable full features, set up a SonarQube Server or SonarCloud

## Running Locally WITH NVD API Key

You must provide a local NVD API Key to run the OWASP scan:

```bash
cd backend/core

# PowerShell (recommended): set environment variable, then run without -D flags
$env:NVD_API_KEY="YOUR_API_KEY_HERE"  # Windows PowerShell
./mvnw clean verify -DskipTests

# Linux/macOS
export NVD_API_KEY="YOUR_API_KEY_HERE"
./mvnw clean verify -DskipTests

# Alternative: pass API key via Maven property (use quoting)
./mvnw clean verify -DskipTests "-Dnvd.api.key=YOUR_API_KEY_HERE" "-Dowasp.cvss.threshold=7.0"
```

The `-Dowasp.cvss.threshold=7.0` parameter makes the build fail if Critical/High severity CVEs are found.

## Running Locally WITHOUT NVD API Key

Run OWASP using cached data (no live NVD update):

```bash
cd backend/core

# Disable NVD update and avoid failing the build
./mvnw verify "-Ddependency-check.skipUpdate=true" "-Ddependency-check.autoUpdate=false" "-Ddependency-check.failBuild=false"
```

### Run Individual Tools
```bash
# Checkstyle only
./mvnw checkstyle:check

# Checkstyle with report script (recommended - generates formatted report)
./run-checkstyle-check.ps1

# SpotBugs only
./mvnw spotbugs:check

# SpotBugs with report script (recommended - generates formatted report)
./run-spotbugs-check.ps1

# PMD only (has many violations in existing code)
./mvnw pmd:check pmd:cpd-check

# PMD with report script (recommended - generates formatted reports)
./run-pmd-check.ps1

# OWASP Dependency-Check (WITH API Key - REQUIRED)
# PowerShell (recommended): set env var, then run without -D flags
$env:NVD_API_KEY="YOUR_API_KEY_HERE"; ./mvnw verify

# Alternative: pass API key via Maven property (quote both -D flags)
./mvnw verify "-Dnvd.api.key=YOUR_API_KEY_HERE" "-Dowasp.cvss.threshold=7.0"

# OWASP Dependency-Check (WITHOUT API Key)
./mvnw verify "-Ddependency-check.skipUpdate=true" "-Ddependency-check.autoUpdate=false" "-Ddependency-check.failBuild=false"

# SonarQube Scanner (requires SonarQube server)
./mvnw org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
  -Dsonar.host.url=YOUR_SERVER_URL \
  -Dsonar.login=YOUR_TOKEN
```

### SpotBugs Report Script

The `run-spotbugs-check.ps1` script provides an easy way to generate a properly formatted SpotBugs report:

**Features:**
- Runs the full Maven verify cycle to ensure SpotBugs analyzes the code
- Extracts the SpotBugs XML output from the target directory
- Formats the XML for better readability with proper indentation
- Saves the report as `spotbugs-result.xml` in the `Static_Analysis_Reports` folder
- Creates a blank report if no bugs are found

**Usage:**
```bash
cd backend/core
./run-spotbugs-check.ps1
```

**Output:**
- Report location: `backend/core/Static_Analysis_Reports/spotbugs-result.xml`
- File is formatted with proper indentation for easy reading
- Shows all detected bugs with file locations and descriptions

### Checkstyle Report Script

The `run-checkstyle-check.ps1` script provides an easy way to generate a properly formatted Checkstyle report:

**Features:**
- Runs Checkstyle analysis on the code
- Extracts the Checkstyle XML output from the target directory
- Formats the XML for better readability with proper indentation
- Saves the report as `checkstyle-result.xml` in the `Static_Analysis_Reports` folder
- Creates a blank report if no violations are found

**Usage:**
```bash
cd backend/core
./run-checkstyle-check.ps1
```

**Output:**
- Report location: `backend/core/Static_Analysis_Reports/checkstyle-result.xml`
- File is formatted with proper indentation for easy reading
- Shows all style violations with file locations and descriptions

### PMD Report Script

The `run-pmd-check.ps1` script provides an easy way to generate properly formatted PMD and CPD reports:

**Features:**
- Runs PMD and CPD checks
- Extracts the PMD XML output from the target directory
- Formats the XML for better readability with proper indentation
- Saves the reports as `pmd-result.xml` and `pmd-cpd-result.xml` in the `Static_Analysis_Reports` folder
- Creates blank reports if no violations are found

**Usage:**
```bash
cd backend/core
./run-pmd-check.ps1
```

**Output:**
- Report locations:
  - `backend/core/Static_Analysis_Reports/pmd-result.xml`
  - `backend/core/Static_Analysis_Reports/pmd-cpd-result.xml`
- Files are formatted with proper indentation for easy reading
- Shows all detected PMD violations and CPD duplications

## GitHub Actions Integration

The workflow `.github/workflows/build-and-analyze.yml` automates static analysis on:
- Push to `main` or `develop` branches
- Pull requests targeting `main` or `develop`

### SonarQube Configuration (Optional)

To enable SonarQube scanning in CI/CD:

1. Set up a SonarQube Server or Cloud instance
2. Add GitHub Actions secrets to your repository:
   - `SONAR_HOST_URL`: URL of your SonarQube server (e.g., `https://sonarqube.example.com`)
   - `SONAR_TOKEN`: Authentication token from SonarQube

3. The workflow will automatically include SonarQube analysis when these secrets are configured

### OWASP Dependency-Check Configuration (Recommended)

To enable full vulnerability scanning without errors:

1. **Get an NVD API Key** (Free, highly recommended):
   - Visit [NVD API Key Registration](https://nvd.nist.gov/developers/request-an-api-key)
   - Request a free API key from NIST
   - You'll receive the key via email

2. **Set up locally** - Add to your environment:
   ```bash
   # Windows PowerShell
   $env:NVD_API_KEY="your-api-key-here"
   
   # Linux/macOS
   export NVD_API_KEY="your-api-key-here"
   ```

3. **Set up in CI/CD** - Add GitHub secret:
   - Go to repository Settings → Secrets and variables → Actions
   - Add new secret: `NVD_API_KEY` with your API key value
   - The workflow will automatically use it

## Getting Your NVD API Key (Step-by-Step Guide)

The National Vulnerability Database (NVD) API requires an API key to fetch the latest vulnerability data. Getting one is **free and takes less than 5 minutes**.

### Step 1: Go to NIST NVD API Registration
1. Open your browser and navigate to: https://nvd.nist.gov/developers/request-an-api-key
2. You should see the "Request an API Key" page

### Step 2: Fill Out the Registration Form
1. **Email Address**: Enter your email address
2. **Organization Name** (optional): Enter your organization/company name
3. **First Name & Last Name**: Enter your name
4. Click **"Submit"** button

### Step 3: Check Your Email
1. You should receive an email from NIST within a few seconds
2. Subject: Usually something like "NVD API Key Request - Your API Key is Ready"
3. The email will contain your **API Key** (a long alphanumeric string)

### Step 4: Copy Your API Key
1. Copy the API key from the email
2. Keep it secure (treat it like a password)
3. Don't commit it to version control or share publicly

### Step 5: Use Your API Key

**Local Development:**
```bash
# Windows PowerShell - Set environment variable (one-time per session)
$env:NVD_API_KEY="your-api-key-here"
./mvnw clean verify -DskipTests -Dowasp.cvss.threshold=7.0

# Linux/macOS - Set environment variable (one-time per session)
export NVD_API_KEY="your-api-key-here"
./mvnw clean verify -DskipTests -Dowasp.cvss.threshold=7.0

# All platforms - Pass directly via Maven parameter (no env var needed)
./mvnw clean verify -DskipTests -Dnvd.api.key="your-api-key-here" -Dowasp.cvss.threshold=7.0
```

**Key Parameters:**
- `-Dnvd.api.key=YOUR_KEY` - Enables NVD database update and verification
- `-Dowasp.cvss.threshold=7.0` - Fails build on High/Critical CVEs (CVSS ≥ 7.0)
- Without these: OWASP uses cached data and doesn't fail the build

**GitHub Actions (CI/CD):**
1. Go to your repository on GitHub
2. Click **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**
4. **Name**: `NVD_API_KEY`
5. **Secret**: Paste your API key
6. Click **Add secret**
7. The GitHub Actions workflow will automatically use it

### Benefits of Using an NVD API Key
- ✅ Access to latest vulnerability data
- ✅ No rate limiting or 403/404 errors
- ✅ Faster scans (10x faster than without API key)
- ✅ More accurate and comprehensive results
- ✅ Works reliably in CI/CD pipelines
- ✅ Completely free

### Troubleshooting NVD API Key

**"Email didn't arrive"**
- Check your spam/junk folder
- Wait a few minutes and refresh your email
- Try requesting another key

**"Invalid API Key error"**
- Verify you copied the entire key (no extra spaces)
- Check for typos in the key
- Make sure the environment variable is set correctly
- Verify with: `echo $env:NVD_API_KEY` (Windows) or `echo $NVD_API_KEY` (Linux/Mac)

**"Still getting 403 errors"**
- Ensure your API key is properly passed to Maven:
  - Check environment variable: `$env:NVD_API_KEY`
  - Or use: `./mvnw org.owasp:dependency-check-maven:check -Dnvd.api.key="your-key"`
- Try removing old cache: `rm -r ~/.m2/repository/.owasp/*` (Linux/Mac) or `rmdir %USERPROFILE%\.m2\repository\.owasp /s` (Windows)

### Analysis Reports

Reports are generated in `backend/core/target/`:
- **Checkstyle**: `checkstyle-result.xml`
- **SpotBugs**: `spotbugsXml.xml`
- **PMD**: `pmd.xml` and `pmd-cpd.xml`
- **OWASP**: `dependency-check-report.html`, `dependency-check-report.json`, `dependency-check-report.xml`
- **SonarQube**: Available on SonarQube server dashboard

Reports are also uploaded as GitHub Actions artifacts for pull requests.

## Configuration Files

### Checkstyle
- Uses `google_checks.xml` (built-in Google style guide)
- To customize, download the config file and reference it in `pom.xml`

### PMD
- Uses `category/java/bestpractices.xml` (built-in ruleset)
- To customize, modify the `rulesets` configuration in `pom.xml`

### OWASP Dependency-Check
- `failBuildOnCVSS`: Fails build if CVSS score ≥ 7.0 (High/Critical)
- `format`: Generates HTML, JSON, and XML reports
- `excludeFromPurge`: Excludes trusted libraries from NVD database purge

### SonarQube
- Properties configured in `pom.xml` under `<properties>`:
  - `sonar.projectKey`: Project identifier
  - `sonar.projectName`: Display name
  - `sonar.exclusions`: Files/folders to exclude from analysis

## Breaking the Build

By default, all analyzers are configured to fail the build if violations are found:
- **Checkstyle**: `<failsOnError>true</failsOnError>`
- **SpotBugs**: `<failOnError>true</failOnError>`
- **PMD**: `<failOnViolation>true</failOnViolation>`
- **OWASP**: `<failBuildOnCVSS>7.0</failBuildOnCVSS>` (fails on High/Critical CVEs)
- **SonarQube**: Optional quality gate via `sonar.qualitygate.wait=true`

To temporarily disable failures (not recommended for production), modify the pom.xml configuration.

## Best Practices

1. **Use SonarQube for IDE during development**: Install the extension for real-time feedback
2. **Fix issues early**: Address violations in pull requests before merging
3. **Customize rules**: Adapt analyzers to your project's coding standards
4. **Monitor trends**: Track SonarQube metrics over time
5. **Regular updates**: Keep analyzer versions current for new checks
6. **Team alignment**: Establish team standards before enforcement
7. **Connect to SonarQube Server**: Enable Connected Mode for full IDE integration and consistency with CI/CD

## Troubleshooting

### Build fails on Checkstyle
- Review violations in console output
- Common issues: indentation, line length, naming conventions
- Fix violations or adjust rules in pom.xml

### SpotBugs reports false positives
- Review findings on SonarQube dashboard
- Add `@SuppressWarnings` annotations when justified
- Document suppressions in code comments

### OWASP Dependency-Check fails
- **Error: "Unable to update 1 or more Cached Web DataSource"** or **"403 or 404 error"**
  - Confirm the NVD API Key is set and valid
  - Use a Maven property explicitly:
    ```bash
    ./mvnw verify -Dnvd.api.key=YOUR_API_KEY_HERE -Dowasp.cvss.threshold=7.0
    ```
  - In PowerShell, add stop-parsing if flags are misread:
    ```bash
    ./mvnw --% verify -Dnvd.api.key=YOUR_API_KEY_HERE -Dowasp.cvss.threshold=7.0
    ```

- **Error: "One or more exceptions occurred during analysis"**
  - Verify API Key is set correctly: `echo $env:NVD_API_KEY` (Windows) or `echo $NVD_API_KEY` (Linux/Mac)
  - Try with explicit API Key: `./mvnw org.owasp:dependency-check-maven:check -Dnvd.api.key=YOUR_KEY`
  - Check internet connectivity to NVD servers

- **Error: "No documents exist"**
  - Local vulnerability database is empty or corrupted
  - Ensure the API key is provided so the database can update
  - Then rerun: `./mvnw verify -Dnvd.api.key=YOUR_API_KEY_HERE -Dowasp.cvss.threshold=7.0`

### SonarQube for IDE not showing issues
- Ensure the extension is installed from VS Code marketplace
- Verify Connected Mode is configured with your SonarQube Server/Cloud
- Check that your authentication token is valid and has project permissions
- Restart VS Code after configuring the connection
- Run `Developer: Reload Window` command if issues persist

### SonarQube scanning skipped in CI/CD
- Verify `SONAR_HOST_URL` and `SONAR_TOKEN` secrets are set in GitHub
- Check SonarQube server is accessible from GitHub Actions runners
- Confirm token has permissions for the project
- Review workflow logs for connection errors

## References

- [SonarQube for IDE Documentation](https://docs.sonarsource.com/sonarlint/vs-code/)
- [SonarQube Connected Mode Guide](https://docs.sonarsource.com/sonarqube/latest/user-guide/sonarlint-connected-mode/)
- [Checkstyle Documentation](https://checkstyle.sourceforge.io/)
- [SpotBugs Documentation](https://spotbugs.readthedocs.io/)
- [PMD Documentation](https://pmd.github.io/)
- [OWASP Dependency-Check Documentation](https://jeremylong.github.io/DependencyCheck/)
- [SonarQube Documentation](https://docs.sonarqube.org/)
- [SonarQube Documentation](https://docs.sonarqube.org/)
