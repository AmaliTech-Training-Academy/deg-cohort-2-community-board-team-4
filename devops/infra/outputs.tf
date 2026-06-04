output "instance_public_ip" {
  description = "Public IP address of the CommunityBoard application host."
  value       = aws_instance.app.public_ip
}

output "instance_public_dns" {
  description = "Public DNS name of the CommunityBoard application host."
  value       = aws_instance.app.public_dns
}

output "key_pair_name" {
  description = "Name of the generated EC2 key pair."
  value       = aws_key_pair.deployer.key_name
}

output "ssh_private_key_path" {
  description = "Local path to the generated SSH private key used by Ansible."
  value       = local_sensitive_file.deployer_key.filename
}

output "ansible_inventory_path" {
  description = "Local path to the generated Ansible inventory file."
  value       = local_file.ansible_inventory.filename
}

output "frontend_url" {
  description = "CommunityBoard frontend URL."
  value       = "http://${aws_instance.app.public_dns}:${var.frontend_port}"
}

output "backend_url" {
  description = "CommunityBoard backend (API) URL."
  value       = "http://${aws_instance.app.public_dns}:${var.backend_port}"
}

output "log_group_name" {
  description = "CloudWatch Logs group the containers ship to (set as awslogs-group in the compose file)."
  value       = aws_cloudwatch_log_group.app.name
}
