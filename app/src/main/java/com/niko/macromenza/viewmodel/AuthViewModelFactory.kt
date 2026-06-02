package com.niko.macromenza.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.niko.macromenza.session.UserSessionManager

class AuthViewModelFactory(
    private val sessionManager: UserSessionManager
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(sessionManager) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}