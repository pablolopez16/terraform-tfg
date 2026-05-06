variable "lambda_jar_path" {
  description = "Path to the lambda jar file"
  type        = string
  default     = "C:/TFG/aws-lambda-01/target/lambda-1.0.0.jar"
}
variable "google_credentials_json" {
  description = "JSON con las credenciales OAuth2 de Google Calendar API (client_id, client_secret, scopes)"
  type        = string
  sensitive   = true
}