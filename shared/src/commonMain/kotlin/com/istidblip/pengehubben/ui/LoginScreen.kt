package com.istidblip.pengehubben.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun LoginScreen(
    onSignIn: suspend (String, String) -> Unit,
    onSignUp: suspend (String, String) -> Unit,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    var emailError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val performLogin = {
        emailError = email.isBlank()
        passwordError = password.isBlank()

        if (!emailError && !passwordError && !isLoading) {
            scope.launch {
                isLoading = true
                error = null
                try {
                    if (isSignUp) {
                        onSignUp(email, password)
                    } else {
                        onSignIn(email, password)
                    }
                    onLoginSuccess()
                } catch (e: Exception) {
                    error = e.message ?: "Authentication failed"
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isSignUp) "Create Account" else "Welcome Back",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isSignUp) "Sign up to start managing your budget" else "Login to continue to Pengehubben",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                if (emailError) emailError = it.isBlank()
            },
            label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth().semantics {
                contentType = ContentType.EmailAddress
            },
            isError = emailError,
            supportingText = if (emailError) {
                { Text("Email cannot be empty") }
            } else null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                if (passwordError) passwordError = it.isBlank()
            },
            label = { Text("Password") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().semantics {
                contentType = ContentType.Password
            },
            isError = passwordError,
            supportingText = if (passwordError) {
                { Text("Password cannot be empty") }
            } else null,
            trailingIcon = {
                val image = if (passwordVisible)
                    Icons.Rounded.Visibility
                else Icons.Rounded.VisibilityOff

                val description = if (passwordVisible) "Hide password" else "Show password"

                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = description)
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { performLogin() }
            ),
            singleLine = true
        )

        if (error != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { performLogin() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading,
            shape = MaterialTheme.shapes.large
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = if (isSignUp) "Sign Up" else "Login",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = {
                isSignUp = !isSignUp
                error = null
                emailError = false
                passwordError = false
            },
            enabled = !isLoading
        ) {
            Text(
                if (isSignUp) "Already have an account? Login" else "Don't have an account? Sign Up",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
@Preview
fun LoginScreenPreview() {
    MaterialTheme {
        LoginScreen(
            onSignIn = { _, _ -> },
            onSignUp = { _, _ -> },
            onLoginSuccess = {}
        )
    }
}
