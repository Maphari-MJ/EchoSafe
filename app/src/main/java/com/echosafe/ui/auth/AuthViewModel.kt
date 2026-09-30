package com.echosafe.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echosafe.data.model.User
import com.echosafe.data.repository.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    private val _authState = MutableLiveData<Result<User>>()
    val authState: LiveData<Result<User>> = _authState

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    fun isUserLoggedIn(): Boolean = authRepository.isUserLoggedIn()

    fun signUp(user: User, password: String) {
        _loading.value = true
        viewModelScope.launch {
            val result = authRepository.registerUser(user, password)
            _loading.value = false
            _authState.value = result
        }
    }

    fun signIn(email: String, password: String) {
        _loading.value = true
        viewModelScope.launch {
            val result = authRepository.loginUser(email, password)
            _loading.value = false
            _authState.value = result
        }
    }
}
