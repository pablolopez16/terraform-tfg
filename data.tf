data "aws_iam_policy" "admin-access" {
    arn = "arn:aws:iam::aws:policy/AdministratorAccess"

}
data "aws_iam_role" "lambda_role" {
    name = "aws-lambda-tfg-role"
  
}

output "admin_policy" {
    value = data.aws_iam_policy.admin-access.policy
}
