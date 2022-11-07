package com.oneandonly.inventationblock.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oneandonly.inventationblock.datasource.Setting
import com.oneandonly.inventationblock.datasource.setting.TokenSetting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TokenViewModel:ViewModel() {

    private var _token: String? = null
    val token get() = _token

    private var tokenSetting: TokenSetting
        = Setting.getInstance().getTokenDataStore()

    fun getToken() {
        viewModelScope.launch {
            _token = tokenSetting.token.first()
            Log.d("Token","tokenSetting ${tokenSetting.token.first()}")
        }
    }

    fun updateToken(token:String) {
        viewModelScope.launch {
            Log.d("Token","updateToken $token")
            tokenSetting.savaToken(token)
        }
    }

}