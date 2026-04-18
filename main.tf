//Creacion Lambda
resource "aws_lambda_function" "aws-lambda-tfg" {
    function_name = "aws-lambda-tfg"
    role = data.aws_iam_role.lambda_role.arn
    runtime = "java17"
    handler = "tfg.prod.Main::handleRequest"

    s3_bucket = aws_s3_bucket.aws-lambda-tfg-bucket.bucket
    s3_key = aws_s3_object.lambda_jar.key
    source_code_hash = aws_s3_object.lambda_jar.etag /*para detectar cambios automaticamente*/
    timeout = 30
}
resource "aws_s3_object" "lambda_jar" {
    bucket = aws_s3_bucket.aws-lambda-tfg-bucket.bucket
    key    = "tfg/lambda_function_code/lambda-1.0.0.jar"
    source = var.lambda_jar_path    
}

resource "aws_s3_bucket" "aws-lambda-tfg-bucket" {
    bucket = "aws-tfg-bucket-plfz"
    tags = {
        "Name" = "state-file"
    }

#region API GateAway

}
