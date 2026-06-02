terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = ">= 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

# One-time bootstrap: creates the S3 bucket + DynamoDB lock table that back
# the per-environment Terraform state (modules/<env>/backend.hcl). Run once
# with a local state before initialising the main configuration.

resource "aws_s3_bucket" "tfstate" {
  bucket = var.state_bucket_name

  tags = {
    Project          = "communityboard"
    ManagedBy        = "terraform"
    TerraformBackend = "true"
  }
}

resource "aws_s3_bucket_versioning" "tfstate" {
  bucket = aws_s3_bucket.tfstate.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "tfstate" {
  bucket = aws_s3_bucket.tfstate.bucket

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_dynamodb_table" "tfstate_locks" {
  name         = var.state_lock_table_name
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "LockID"

  attribute {
    name = "LockID"
    type = "S"
  }

  tags = {
    Project          = "communityboard"
    ManagedBy        = "terraform"
    TerraformBackend = "true"
  }
}
