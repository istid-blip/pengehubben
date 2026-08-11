package com.istidblip.pengehubben.auth

import com.istidblip.pengehubben.Supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AuthRepository {
    val sessionStatus = Supabase.client.auth.sessionStatus
    
    val isAuthenticated: Flow<Boolean> = sessionStatus.map { it is SessionStatus.Authenticated }

    suspend fun signUp(email: String, password: String) {
        try {
            Supabase.client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
        } catch (e: Exception) {
            println("Error signing up: ${e.message}")
        }
    }

    suspend fun signIn(email: String, password: String) {
        try {
            Supabase.client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
        } catch (e: Exception) {
            println("Error signing in: ${e.message}")
        }
    }

    suspend fun signOut() {
        try {
            Supabase.client.auth.signOut()
        } catch (e: Exception) {
            println("Error signing out: ${e.message}")
        }
    }
}
