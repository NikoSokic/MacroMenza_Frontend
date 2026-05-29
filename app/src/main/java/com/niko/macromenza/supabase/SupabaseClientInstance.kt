
package com.niko.macromenza.supabase

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth

object SupabaseClientInstance {

    val client = createSupabaseClient(
        supabaseUrl = "https://nncjmtrxderijntybnhb.supabase.co/rest/v1/",
        supabaseKey = "sb_publishable_vs4HOEwugnBnhHtMBUV7gw_nO16Wjah"
    ) {
        install(Auth)
    }
}