package com.genesys.mobile.messenger.sdk.gcmessengersdksample.presentation.chat_form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.genesys.mobile.messenger.sdk.gcmessengersdksample.data.repositories.JsonSampleRepository

class SampleFormViewModelFactory(private val repository: JsonSampleRepository) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SampleFormViewModel(repository) as T
    }
}