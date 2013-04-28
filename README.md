# User Boilerplate for Play Framework & Scala
Boilerplate code for pages which requires user management. This code is mainly for my own benefit but it should be easy to adapt for any purpose. There is no license, use it as you like.

### This code provides:
 * Model for User creation, authentication & verification.
 * Authenticated requests with default or custom verification methods.
 * Controller for signing in and out.
 * Example form to validate user credentials.
 * Example session creation and verification.

### By default this code:
 * Uses MySQL databases. (SQL-code included in evolutions)
 * hashes passwords using PBKDF2 with HMAC SHA1.
 * Stores email in the session cookie as the unique key.
 * Targets Play 2.1.1.

## User model
### Create a user
```scala
models.User.create("test@example.com", "topSecretPassword".toCharArray)
```

### Authenticate a user
```scala
models.User.authenticate("test@example.com", "topSecretPassword".toCharArray)
```

### Default method used for authenticated requests
```scala
models.User.authenticateEmail("test@example.com")
```

## User request
### Default authentication method
```scala
def protectedPage = userRequest() { implicit request =>
    Ok(views.html.secret())
}
```

### Explicit authentication method
```scala
def protectedPage = userRequest(models.User.authenticateEmail) { implicit request =>
    Ok(views.html.secret())
}
```