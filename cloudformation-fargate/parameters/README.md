## Parameter Files

These files are JSON because the AWS CLI accepts them directly with:

```powershell
--parameters file://path-to-file.json
```

JSON does not support inline comments, so this file explains what each
parameter file is for and what values need to be replaced before deployment.

### Files

1. `dev-networking.json`
- baseline networking parameters for the `dev` environment

2. `dev-data.json`
- database and JWT values for the `dev` environment

3. `dev-platform.json`
- ECR repository name and log retention for the `dev` environment

4. `dev-service.json`
- image URI and service/runtime settings for the `dev` environment

5. `cfdemo-networking.json`
- networking parameters for the parallel CloudFormation demo environment

6. `cfdemo-data.json`
- database and JWT values for the parallel CloudFormation demo environment

7. `cfdemo-platform.json`
- ECR repository name and log retention for the parallel CloudFormation demo environment

8. `cfdemo-service.json`
- image URI and service/runtime settings for the parallel CloudFormation demo environment

### Placeholders that must be replaced

#### In `*-data.json`

- `DatabaseMasterPassword`
  - real PostgreSQL master password for the new RDS instance

- `JwtSecret`
  - long random string used by the backend for JWT signing

#### In `*-service.json`

- `BackendImageUri`
  - full ECR image URI including the tag
  - example:
    - `331738867837.dkr.ecr.us-east-1.amazonaws.com/careconnect-backend-cfdemo:cfdemo`

### Runtime notes

- `SpringProfile` is set to `dev` because the backend’s working cloud deployment
  path currently uses the dev profile with externalized env vars.
- `ContainerPort` is `8081` because that is the working ECS port for this app.
- `HealthCheckPath` is `/v1/api/test/health` because that is the endpoint the
  ALB uses to determine task health.

### Parallel deployment guidance

Use a separate environment name like `cfdemo` when you need a second deployment
that does not interfere with an existing manual environment.

Keep these unique across parallel environments:

- CloudFormation stack names
- ECR repository names, if you create per-environment repos
- Docker image tags
- Secrets values
