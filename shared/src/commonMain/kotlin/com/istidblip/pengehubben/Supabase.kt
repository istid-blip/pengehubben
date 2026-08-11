package com.istidblip.pengehubben

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

object Supabase {
    val SUPABASE_URL = BuildConfig.SUPABASE_URL
    val SUPABASE_ANON_KEY = BuildConfig.SUPABASE_ANON_KEY

    init {
        if (SUPABASE_URL.contains("your-project") || SUPABASE_ANON_KEY.contains("your-anon-key")) {
            println("****************************************************************")
            println("ERROR: Supabase URL or Anon Key is using placeholder values!")
            println("Please update local.properties with your real Supabase credentials.")
            println("Current URL: $SUPABASE_URL")
            println("****************************************************************")
        }
    }

    val client by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
        }
    }
}
