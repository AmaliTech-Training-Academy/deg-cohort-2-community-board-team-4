variable "aws_region" {
  description = "AWS region for the Terraform state backend resources."
  type        = string
  default     = "us-west-1"
}

variable "state_bucket_name" {
  description = "Globally-unique S3 bucket name for Terraform state. Must match modules/<env>/backend.hcl."
  type        = string
  default     = "communityboard-tfstate"
}

variable "state_lock_table_name" {
  description = "DynamoDB table name for Terraform state locking. Must match modules/<env>/backend.hcl."
  type        = string
  default     = "communityboard-tfstate-locks"
}
