#region Lambda
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
     GOOGLE_CREDENTIALS_SECRET_ARN  = aws_secretsmanager_secret.google_credentials.arn
     FRONTEND_URL = "https://${aws_cloudfront_distribution.frontend.domain_name}"
     ICS_CACHE_BUCKET = aws_s3_bucket.aws-lambda-tfg-bucket.bucket
     COGNITO_USER_POOL_ID = aws_cognito_user_pool.tfg.id
     COGNITO_CLIENT_ID    = aws_cognito_user_pool_client.tfg.id
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

#endregion
#region API GateAway
resource "aws_apigatewayv2_api" "api-gateway-tfg" {
  name          = "api-gateway-tfg"
  protocol_type = "HTTP"

   cors_configuration {
    allow_origins = ["http://aws-tfg-frontend-plfz.s3-website-us-east-1.amazonaws.com", "http://localhost:4200", "https://${aws_cloudfront_distribution.frontend.domain_name}"]
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
  authorization_type = "JWT"
  authorizer_id      = aws_apigatewayv2_authorizer.cognito.id
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
      Action   = ["dynamodb:GetItem", "dynamodb:PutItem", "dynamodb:DeleteItem","dynamodb:Scan"]
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

resource "null_resource" "frontend_deploy" {
  triggers = {
    src_hash = sha1(join("", [
      for f in fileset("${path.module}/angular-frontend/src", "**") :
      filesha1("${path.module}/angular-frontend/src/${f}")
    ]))
  }

  provisioner "local-exec" {
    interpreter = ["PowerShell", "-Command"]
    command = "cd ${path.module}/angular-frontend; npm install; ng build -- --configuration production; aws s3 sync dist/angular-frontend/browser/ s3://${aws_s3_bucket.frontend.bucket}/ --delete"
  }

  depends_on = [aws_s3_bucket_policy.frontend]
}
#endregion

#region Secrets Manager

resource "aws_secretsmanager_secret" "google_credentials" {
  name        = "tfg/google-credentials"
  description = "Credenciales OAuth2 de Google Calendar API (client_id, client_secret, scopes)"
}
resource "aws_secretsmanager_secret_version" "google_credentials" {
  secret_id     = aws_secretsmanager_secret.google_credentials.id
  secret_string = var.google_credentials_json
}

resource "aws_iam_role_policy" "lambda_secrets_manager" {
  name = "lambda-secrets-manager-policy"
  role = data.aws_iam_role.lambda_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["secretsmanager:GetSecretValue"]
      Resource = aws_secretsmanager_secret.google_credentials.arn
    }]
  })
}
#endregion 

#region EventBridge

resource "aws_cloudwatch_event_rule" "calendar_refresh" {
  name                = "tfg-calendar-refresh"
  schedule_expression = "rate(15 minutes)"
}

resource "aws_cloudwatch_event_target" "calendar_refresh" {
  rule = aws_cloudwatch_event_rule.calendar_refresh.name
  arn  = aws_lambda_function.aws-lambda-tfg.arn
}

resource "aws_lambda_permission" "eventbridge" {
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.aws-lambda-tfg.function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.calendar_refresh.arn
}

resource "aws_iam_role_policy" "lambda_s3_ics" {
  name = "lambda-s3-ics-policy"
  role = data.aws_iam_role.lambda_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["s3:PutObject", "s3:GetObject"]
      Resource = "${aws_s3_bucket.aws-lambda-tfg-bucket.arn}/ics/*"
    }]
  })
}

#endregion

#region CloudFront

resource "aws_cloudfront_distribution" "frontend" {
  enabled             = true
  default_root_object = "index.html"
  price_class         = "PriceClass_100"

 origin {
  domain_name = aws_s3_bucket_website_configuration.frontend.website_endpoint
  origin_id   = "s3-frontend"

  custom_origin_config {
    http_port              = 80
    https_port             = 443
    origin_protocol_policy = "http-only"
    origin_ssl_protocols   = ["TLSv1.2"]
  }
}

  default_cache_behavior {
    target_origin_id       = "s3-frontend"
    viewer_protocol_policy = "redirect-to-https"
    allowed_methods        = ["GET", "HEAD"]
    cached_methods         = ["GET", "HEAD"]

    forwarded_values {
      query_string = false
      cookies { forward = "none" }
    }

    min_ttl     = 0
    default_ttl = 3600
    max_ttl     = 86400
  }

  custom_error_response {
    error_code         = 403
    response_code      = 200
    response_page_path = "/index.html"
  }

  custom_error_response {
    error_code         = 404
    response_code      = 200
    response_page_path = "/index.html"
  }

  restrictions {
    geo_restriction { restriction_type = "none" }
  }

  viewer_certificate {
    cloudfront_default_certificate = true
  }

  depends_on = [aws_s3_bucket_policy.frontend]
}

output "cloudfront_url" {
  value = "https://${aws_cloudfront_distribution.frontend.domain_name}"
}

#endregion CloudFront


