terraform {
  backend "s3" {
    bucket = "aws-tfg-bucket-plfz"
    key    = "tfg/state/terraform.tfstate"
    region = "us-east-1"
  }
}