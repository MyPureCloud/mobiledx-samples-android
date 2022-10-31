package com.genesys.cloud.messenger.sample.chat_form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.genesys.cloud.messenger.sample.data.repositories.JsonSampleRepository

class SampleFormViewModelFactory(private val repository: JsonSampleRepository) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SampleFormViewModel(repository) as T
    }
}