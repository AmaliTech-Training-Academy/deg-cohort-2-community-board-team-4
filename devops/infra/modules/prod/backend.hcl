bucket         = "communityboard-tfstate"
key            = "communityboard/prod/terraform.tfstate"
region         = "us-west-1"
dynamodb_table = "communityboard-tfstate-locks"
encrypt        = true
