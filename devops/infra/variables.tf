variable "project_name" {
  description = "Project name used as a prefix for all resource names and tags."
  type        = string
  default     = "communityboard"
}

variable "environment" {
  description = "Deployment environment (dev, test, prod). Set per environment via modules/<env>/<env>.tfvars."
  type        = string

  validation {
    condition     = contains(["dev", "test", "prod"], var.environment)
    error_message = "environment must be one of: dev, test, prod."
  }
}

variable "aws_region" {
  description = "AWS region to deploy into."
  type        = string
  default     = "us-west-1"
}

variable "instance_type" {
  description = "EC2 instance type for the application host."
  type        = string
  default     = "t2.micro"
}

variable "allowed_ssh_cidr" {
  description = "CIDR block allowed to SSH into the application host."
  type        = string
  default     = "0.0.0.0/0"
}

variable "frontend_port" {
  description = "Port the CommunityBoard frontend (UI) is exposed on."
  type        = number
  default     = 3000
}

variable "backend_port" {
  description = "Port the CommunityBoard backend (API) is exposed on."
  type        = number
  default     = 8080
}

variable "log_group_name" {
  description = "CloudWatch Logs group the containers ship to via the awslogs driver. Empty = use the environment name (so the 'test' env resolves to 'test', matching awslogs-group in docker-compose-staging.yml)."
  type        = string
  default     = ""
}

variable "log_retention_days" {
  description = "Retention period (in days) for the CloudWatch log group."
  type        = number
  default     = 14
}
