## CareConnect Fargate CloudFormation

This directory contains a clean CloudFormation stack set for the CareConnect
backend running on:

- Amazon ECS Fargate
- Application Load Balancer
- Amazon RDS PostgreSQL
- Amazon ECR

It does not depend on the older `cloudformation/` or `terraform_aws/` layouts.

This stack set was validated by deploying a parallel `cfdemo` environment in
the same AWS account without interfering with the existing manually created
Fargate deployment.

### Stack order

1. `01-networking.yaml`
2. `02-data.yaml`
3. `03-platform.yaml`
4. Build and push the backend image to ECR
5. `04-service.yaml`

### What each stack owns

1. `01-networking.yaml`
- VPC
- public subnets for ALB and ECS
- private subnets for RDS
- route tables
- internet gateway
- ALB / ECS / RDS security groups

2. `02-data.yaml`
- PostgreSQL RDS instance
- DB subnet group
- Secrets Manager secret for DB password
- Secrets Manager secret for JWT secret

3. `03-platform.yaml`
- ECR repository
- ECS cluster
- ECS task execution role
- ECS task role
- CloudWatch log group for the backend container

4. `04-service.yaml`
- Application Load Balancer
- target group
- listener
- ECS task definition
- ECS service
- app environment variable and secret wiring

### Design choices

- ALB is public on HTTP port `80`
- ECS tasks run in public subnets with public IPs enabled to avoid NAT costs
- RDS runs in private subnets
- Database and application secrets are stored in Secrets Manager
- ECS task execution role reads secrets and writes logs

### Required application contract

The templates assume the backend uses these environment variables:

- `SPRING_PROFILES_ACTIVE`
- `SERVER_PORT`
- `JDBC_URI`
- `DB_USER`
- `DB_PASSWORD`
- `SECURITY_JWT_SECRET`
- `APP_FRONTEND_BASE_URL`
- `CORS_ALLOWED_LIST`
- `SPRING_FLYWAY_ENABLED`
- `SPRING_JPA_HIBERNATE_DDL_AUTO`

The ALB health check path is:

- `/v1/api/test/health`

### Parameter files

Parameter files live under [`parameters`](C:/Dev/SWEN670/2026_spring_careconnect/cloudformation-fargate/parameters).
Because JSON does not support inline comments, the detailed parameter guide is
in [`parameters/README.md`](C:/Dev/SWEN670/2026_spring_careconnect/cloudformation-fargate/parameters/README.md).

### Example deploy commands

Create the networking stack:

```powershell
aws cloudformation create-stack `
  --stack-name careconnect-networking-dev `
  --template-body file://.\templates\01-networking.yaml `
  --parameters file://.\parameters\dev-networking.json `
  --capabilities CAPABILITY_NAMED_IAM
```

Create the data stack:

```powershell
aws cloudformation create-stack `
  --stack-name careconnect-data-dev `
  --template-body file://.\templates\02-data.yaml `
  --parameters file://.\parameters\dev-data.json `
  --capabilities CAPABILITY_NAMED_IAM
```

Create the platform stack:

```powershell
aws cloudformation create-stack `
  --stack-name careconnect-platform-dev `
  --template-body file://.\templates\03-platform.yaml `
  --parameters file://.\parameters\dev-platform.json `
  --capabilities CAPABILITY_NAMED_IAM
```

Get the repository URI:

```powershell
aws cloudformation describe-stacks `
  --stack-name careconnect-platform-dev `
  --query "Stacks[0].Outputs[?OutputKey=='EcrRepositoryUri'].OutputValue" `
  --output text
```

Build and push the backend image after packaging the jar:

```powershell
cd C:\Dev\SWEN670\2026_spring_careconnect\backend\core
.\mvnw.cmd clean package -Pdocker -DskipTests

$REGION = "us-east-1"
$ACCOUNT_ID = (aws sts get-caller-identity --query Account --output text).Trim()
$IMAGE_URI = "$ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com/careconnect-backend:dev"

aws ecr get-login-password --region $REGION | docker login --username AWS --password-stdin "$ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com"
docker build -t careconnect-backend:dev .
docker tag careconnect-backend:dev $IMAGE_URI
docker push $IMAGE_URI
```

Create the service stack:

```powershell
aws cloudformation create-stack `
  --stack-name careconnect-service-dev `
  --template-body file://.\templates\04-service.yaml `
  --parameters `
    ParameterKey=Environment,ParameterValue=dev `
    ParameterKey=BackendImageUri,ParameterValue="$IMAGE_URI" `
    ParameterKey=SpringProfile,ParameterValue=dev `
    ParameterKey=FrontendBaseUrl,ParameterValue=http://localhost:3000 `
    ParameterKey=CorsAllowedList,ParameterValue="http://localhost:*,http://127.0.0.1:*" `
    ParameterKey=ContainerPort,ParameterValue=8081 `
    ParameterKey=DesiredCount,ParameterValue=1 `
    ParameterKey=TaskCpu,ParameterValue=1024 `
    ParameterKey=TaskMemory,ParameterValue=3072 `
    ParameterKey=HealthCheckPath,ParameterValue=/v1/api/test/health `
    ParameterKey=HealthCheckGracePeriodSeconds,ParameterValue=180 `
  --capabilities CAPABILITY_NAMED_IAM
```

Get the ALB DNS name:

```powershell
aws cloudformation describe-stacks `
  --stack-name careconnect-service-dev `
  --query "Stacks[0].Outputs[?OutputKey=='LoadBalancerDnsName'].OutputValue" `
  --output text
```

Run the frontend against the deployed backend:

```powershell
flutter run --dart-define=BACKEND_URL=http://<alb-dns-name>
```

### Parallel environment pattern

To test changes without touching an existing environment:

1. copy the `dev-*.json` parameter files
2. create a new environment name like `cfdemo`
3. use unique stack names such as:
- `careconnect-networking-cfdemo`
- `careconnect-data-cfdemo`
- `careconnect-platform-cfdemo`
- `careconnect-service-cfdemo`
4. use a distinct ECR image tag such as `cfdemo`

This keeps the old and new ALBs, ECS services, clusters, and databases
separate.
