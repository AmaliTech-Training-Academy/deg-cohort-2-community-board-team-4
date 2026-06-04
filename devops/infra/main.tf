terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = ">= 5.0"
    }
    tls = {
      source  = "hashicorp/tls"
      version = ">= 4.0"
    }
    local = {
      source  = "hashicorp/local"
      version = ">= 2.0"
    }
  }


  backend "s3" {}
}

provider "aws" {
  region = var.aws_region
}

locals {
  name_prefix      = "${var.project_name}-${var.environment}"
  ssh_user         = "ec2-user"
  key_name         = "${local.name_prefix}-deployer"
  private_key_path = abspath("${path.module}/../ansible/${local.key_name}.pem")
  inventory_path   = abspath("${path.module}/../ansible/inventory.ini")
  inventory_tmpl   = abspath("${path.module}/../ansible/inventory.tmpl")

  # Log group the containers ship to via the awslogs driver. Defaults to the
  # environment name, so the 'test' env => "test" — matching awslogs-group in
  # docker-compose-staging.yml. Override with var.log_group_name.
  log_group_name = coalesce(var.log_group_name, var.environment)

  tags = {
    Project     = var.project_name
    Environment = var.environment
    ManagedBy   = "terraform"
  }
}

# --- Networking (use the account's default VPC / subnets) ---------------------

data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

# --- Security group: SSH + the CommunityBoard frontend/backend ports ----------

resource "aws_security_group" "app" {
  name        = "${local.name_prefix}-sg"
  description = "CommunityBoard access for ${local.name_prefix}"
  vpc_id      = data.aws_vpc.default.id

  tags = merge(local.tags, { Name = "${local.name_prefix}-sg" })
}

resource "aws_vpc_security_group_ingress_rule" "ssh" {
  security_group_id = aws_security_group.app.id
  cidr_ipv4         = var.allowed_ssh_cidr
  from_port         = 22
  to_port           = 22
  ip_protocol       = "tcp"
  description       = "SSH"
}

resource "aws_vpc_security_group_ingress_rule" "frontend" {
  security_group_id = aws_security_group.app.id
  cidr_ipv4         = "0.0.0.0/0"
  from_port         = var.frontend_port
  to_port           = var.frontend_port
  ip_protocol       = "tcp"
  description       = "CommunityBoard frontend (UI)"
}

resource "aws_vpc_security_group_ingress_rule" "backend" {
  security_group_id = aws_security_group.app.id
  cidr_ipv4         = "0.0.0.0/0"
  from_port         = var.backend_port
  to_port           = var.backend_port
  ip_protocol       = "tcp"
  description       = "CommunityBoard backend (API)"
}

resource "aws_vpc_security_group_egress_rule" "all" {
  security_group_id = aws_security_group.app.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
  description       = "Allow all outbound"
}

# --- SSH key pair (generated locally, used by Ansible) ------------------------

resource "tls_private_key" "deployer" {
  algorithm = "RSA"
  rsa_bits  = 4096
}

resource "local_sensitive_file" "deployer_key" {
  filename        = local.private_key_path
  content         = tls_private_key.deployer.private_key_pem
  file_permission = "0400"
}

resource "aws_key_pair" "deployer" {
  key_name   = local.key_name
  public_key = tls_private_key.deployer.public_key_openssh
  tags       = local.tags
}

# --- EC2 application host -----------------------------------------------------

data "aws_ami" "amazon_linux" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["amzn2-ami-hvm-*-x86_64-gp2"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

resource "aws_instance" "app" {
  ami                         = data.aws_ami.amazon_linux.id
  instance_type               = var.instance_type
  key_name                    = aws_key_pair.deployer.key_name
  vpc_security_group_ids      = [aws_security_group.app.id]
  subnet_id                   = data.aws_subnets.default.ids[0]
  associate_public_ip_address = true
  iam_instance_profile        = aws_iam_instance_profile.app.name

  tags = merge(local.tags, { Name = "${local.name_prefix}-app" })
}

# --- CloudWatch Logs ----------------------------------------------------------
# Containers ship their logs here via the Docker `awslogs` driver (see the
# `logging:` blocks in docker-compose-staging.yml). Terraform owns the group so
# it has a managed retention; the compose still sets awslogs-create-group=true,
# which is a harmless no-op once the group exists.

resource "aws_cloudwatch_log_group" "app" {
  name              = local.log_group_name
  retention_in_days = var.log_retention_days
  tags              = merge(local.tags, { Name = local.log_group_name })
}

# --- IAM: let the EC2 host push container logs to CloudWatch ------------------
# Without this instance profile the awslogs driver can't authenticate and
# containers fail to start.

data "aws_iam_policy_document" "ec2_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "app" {
  name               = "${local.name_prefix}-ec2"
  assume_role_policy = data.aws_iam_policy_document.ec2_assume.json
  tags               = local.tags
}

data "aws_iam_policy_document" "cloudwatch_logs" {
  statement {
    sid    = "AwslogsDriver"
    effect = "Allow"
    actions = [
      "logs:CreateLogGroup",
      "logs:CreateLogStream",
      "logs:PutLogEvents",
      "logs:DescribeLogStreams",
    ]
    resources = [
      aws_cloudwatch_log_group.app.arn,
      "${aws_cloudwatch_log_group.app.arn}:*",
    ]
  }
}

resource "aws_iam_role_policy" "cloudwatch_logs" {
  name   = "${local.name_prefix}-cw-logs"
  role   = aws_iam_role.app.id
  policy = data.aws_iam_policy_document.cloudwatch_logs.json
}

resource "aws_iam_instance_profile" "app" {
  name = "${local.name_prefix}-ec2"
  role = aws_iam_role.app.name
  tags = local.tags
}

# --- Ansible inventory (generated from the instance public IP) ----------------

resource "local_file" "ansible_inventory" {
  filename = local.inventory_path
  content = templatefile(local.inventory_tmpl, {
    public_ip        = aws_instance.app.public_ip
    ssh_user         = local.ssh_user
    private_key_path = local_sensitive_file.deployer_key.filename
  })
}
