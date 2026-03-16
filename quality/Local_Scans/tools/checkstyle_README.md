# Checkstyle Integration

## Overview

Checkstyle enforces Java coding standards across the CareConnect
codebase.

It validates source files against a configurable ruleset to ensure
consistent formatting, naming conventions, and structural best
practices.

------------------------------------------------------------------------

## Tool Version

Checkstyle 10.12.4

Jar file:

checkstyle-10.12.4-all.jar

------------------------------------------------------------------------

## What Checkstyle Detects

Checkstyle identifies violations such as:

-   Incorrect indentation
-   Missing Javadoc
-   Naming convention violations
-   Unused imports
-   Line length violations
-   Code formatting inconsistencies

------------------------------------------------------------------------

## Execution

Checkstyle is executed via:

quality/Local_Scans/tools/checkstyle/check_checkstyle.sh

Example invocation:

java -jar checkstyle-10.12.4-all.jar -c config.xml src/

------------------------------------------------------------------------

## Output

Results are parsed and integrated into the **local HTML report**.

Native output example:

quality/analysis/raw/checkstyle.xml

------------------------------------------------------------------------

## Configuration

Rules are defined in:

quality/config/checkstyle.xml

------------------------------------------------------------------------

## Purpose in the Quality Gate

Checkstyle ensures:

-   Consistent code formatting
-   Maintainable source code
-   Standardized Java style across contributors
