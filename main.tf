//Creacion Lambda
/*resource "aws_lambda_function" "aws-lambda-tfg" {
    function_name = "aws-lambda-tfg"
    role = data.aws_iam_role.lambda_role.arn
    runtime = "java17"
    handler = "tfg.prod.Main::handleRequest"

    filename = "C:/TFG/aws-lambda-01/target/lambda-1.0.0-shaded.jar"
    source_code_hash = filebase64sha256("C:/TFG/aws-lambda-01/target/lambda-1.0.0-shaded.jar")
  
}*/

data "aws_lambda_function" "aws-lambda-tf-01" {
    function_name = "aws-lambda-tfg"
}
resource "aws_s3_bucket" "aws-lambda-tfg-bucket" {
    bucket = "aws-tfg-bucket-plfz"
    tags = {
        "Name" = "state-file"
    }
}
