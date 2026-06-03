#region Cognito

resource "aws_cognito_user_pool" "tfg" {
  name = "tfg-user-pool"

  password_policy {
    minimum_length    = 8
    require_uppercase = true
    require_lowercase = true
    require_numbers   = true
    require_symbols   = false
  }

  auto_verified_attributes = ["email"]
  username_attributes      = ["email"]

  username_configuration {
    case_sensitive = false
  }

  schema {
    name                = "email"
    attribute_data_type = "String"
    required            = true
    mutable             = true
    string_attribute_constraints {
      min_length = 5
      max_length = 254
    }
  }

  account_recovery_setting {
    recovery_mechanism {
      name     = "verified_email"
      priority = 1
    }
  }
}

resource "aws_cognito_user_pool_client" "tfg" {
  name         = "tfg-angular-client"
  user_pool_id = aws_cognito_user_pool.tfg.id

  generate_secret = false

  explicit_auth_flows = [
    "ALLOW_USER_PASSWORD_AUTH",
    "ALLOW_REFRESH_TOKEN_AUTH",
    "ALLOW_USER_SRP_AUTH"
  ]

  callback_urls = [
    "https://${aws_cloudfront_distribution.frontend.domain_name}",
    "http://localhost:4200"
  ]

  logout_urls = [
    "https://${aws_cloudfront_distribution.frontend.domain_name}",
    "http://localhost:4200"
  ]

  allowed_oauth_flows_user_pool_client = true
  allowed_oauth_flows                  = ["code"]
  allowed_oauth_scopes                 = ["email", "openid", "profile"]

  supported_identity_providers = ["COGNITO"]

  token_validity_units {
    access_token  = "hours"
    id_token      = "hours"
    refresh_token = "days"
  }

  access_token_validity  = 1
  id_token_validity      = 1
  refresh_token_validity = 30
}

resource "aws_cognito_user_pool_domain" "tfg" {
  domain       = "tfg-app-plfz"
  user_pool_id = aws_cognito_user_pool.tfg.id
}

resource "aws_apigatewayv2_authorizer" "cognito" {
  api_id           = aws_apigatewayv2_api.api-gateway-tfg.id
  authorizer_type  = "JWT"
  identity_sources = ["$request.header.Authorization"]
  name             = "cognito-jwt-authorizer"

  jwt_configuration {
    audience = [aws_cognito_user_pool_client.tfg.id]
    issuer   = "https://cognito-idp.us-east-1.amazonaws.com/${aws_cognito_user_pool.tfg.id}"
  }
}

output "cognito_user_pool_id" {
  value = aws_cognito_user_pool.tfg.id
}

output "cognito_client_id" {
  value = aws_cognito_user_pool_client.tfg.id
}

output "cognito_hosted_ui_url" {
  value = "https://${aws_cognito_user_pool_domain.tfg.domain}.auth.us-east-1.amazoncognito.com"
}

#endregion Cognito