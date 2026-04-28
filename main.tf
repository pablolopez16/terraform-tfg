//Creacion Lambda
resource "aws_lambda_function" "aws-lambda-tfg" {
  function_name = "aws-lambda-tfg"
  role          = data.aws_iam_role.lambda_role.arn
  runtime       = "java17"
  handler       = "tfg.prod.Main::handleRequest"

  s3_bucket        = aws_s3_bucket.aws-lambda-tfg-bucket.bucket
  s3_key           = aws_s3_object.lambda_jar.key
  source_code_hash = filebase64sha256(var.lambda_jar_path) /*para detectar cambios automaticamente*/
  memory_size = 512
  timeout          = 30
  environment {
  variables = {
    DYNAMODB_TABLE = aws_dynamodb_table.calendar_accounts.name
     DYNAMODB_MERGE_TABLE = aws_dynamodb_table.merge_configs.name
  }

  }
}
resource "aws_s3_object" "lambda_jar" {
  bucket = aws_s3_bucket.aws-lambda-tfg-bucket.bucket
  key    = "tfg/lambda_function_code/lambda-1.0.0.jar"
  source = var.lambda_jar_path
  etag =  filemd5(var.lambda_jar_path) // para que se cambie cuando sea
  
}

resource "aws_s3_bucket" "aws-lambda-tfg-bucket" {
  bucket = "aws-tfg-bucket-plfz"
  tags = {
    "Name" = "state-file"
  }

}
#region API GateAway
resource "aws_apigatewayv2_api" "api-gateway-tfg" {
  name          = "api-gateway-tfg"
  protocol_type = "HTTP"

   cors_configuration {
    allow_origins = ["*"]   # cambiar a la URL del S3 frontend cuando esté listo
    allow_methods = ["GET", "POST", "DELETE", "OPTIONS"]
    allow_headers = ["Content-Type", "Authorization"]
    max_age       = 300
  }
}

// Connects API Gateaway with Lambda
resource "aws_apigatewayv2_integration" "lambda_integration" {
  api_id             = aws_apigatewayv2_api.api-gateway-tfg.id
  integration_type   = "AWS_PROXY"
  integration_uri    = aws_lambda_function.aws-lambda-tfg.invoke_arn
  integration_method = "POST"
  payload_format_version = "2.0"
}

resource "aws_apigatewayv2_route" "api-gateaway-route" {
  api_id    = aws_apigatewayv2_api.api-gateway-tfg.id
  route_key = "$default" // Para que Spring Boot gestione todas las rutas
  // De esta manera no hace falta crear muchos recursos uno para cada llamada
  target = "integrations/${aws_apigatewayv2_integration.lambda_integration.id}"
}

// Permissions for API Gateaway to invoke Lambda
// The /* part allows invocation from any stage, method and resource path within API Gateway.
resource "aws_lambda_permission" "api-gateway-permissions" {
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.aws-lambda-tfg.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.api-gateway-tfg.execution_arn}/*/*"
}

// Stage : Version publicada de mi api
resource "aws_apigatewayv2_stage" "api-gateway-stage" {
  api_id      = aws_apigatewayv2_api.api-gateway-tfg.id
  name        = "$default"// para que aws no añada prefijo al path
  auto_deploy = true


}
#region DynamboDB
resource "aws_dynamodb_table" "calendar_accounts" {
  name         = "tfg-calendar-accounts"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "account_id"
   attribute {
    name = "account_id"
    type = "S"
  }
  tags = {
    Name = "tfg-calendar-accounts"
  }
}
resource "aws_dynamodb_table" "merge_configs" {
  name         = "tfg-merge-configs"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "merge_id"
   attribute {
    name = "merge_id"
    type = "S"
  }
  tags = {
    Name = "tfg-merge-configs"
  }
}

// Política IAM para que Lambda acceda a DynamoDB
resource "aws_iam_role_policy" "lambda_dynamodb" {
  name = "lambda-dynamodb-policy"
  role = data.aws_iam_role.lambda_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["dynamodb:GetItem", "dynamodb:PutItem", "dynamodb:DeleteItem"]
      Resource = aws_dynamodb_table.calendar_accounts.arn
    }]
  })
}

resource "aws_iam_role_policy" "lambda_dynamodb_merge" {
  name = "lambda-dynamodb-merge-policy"
  role = data.aws_iam_role.lambda_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["dynamodb:GetItem", "dynamodb:PutItem", "dynamodb:DeleteItem"]
      Resource = aws_dynamodb_table.merge_configs.arn
    }]
  })
}
#endregion


#region FrontEnd
# S3 para frontend SPA Angular
resource "aws_s3_bucket" "frontend" {
  bucket = "aws-tfg-frontend-plfz"
}

resource "aws_s3_bucket_public_access_block" "frontend" {
  bucket                  = aws_s3_bucket.frontend.id
  block_public_acls       = false
  block_public_policy     = false
  ignore_public_acls      = false
  restrict_public_buckets = false
}

resource "aws_s3_bucket_website_configuration" "frontend" {
  bucket = aws_s3_bucket.frontend.id
  index_document { suffix = "index.html" }
  error_document  { key    = "index.html" }
}

resource "aws_s3_bucket_policy" "frontend" {
  bucket     = aws_s3_bucket.frontend.id
  depends_on = [aws_s3_bucket_public_access_block.frontend]
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = "*"
      Action    = "s3:GetObject"
      Resource  = "${aws_s3_bucket.frontend.arn}/*"
    }]
  })
}

output "frontend_url" {
  value = aws_s3_bucket_website_configuration.frontend.website_endpoint
}

output "api_url" {
  value = aws_apigatewayv2_stage.api-gateway-stage.invoke_url
}
#endregion


