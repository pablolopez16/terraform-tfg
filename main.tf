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


