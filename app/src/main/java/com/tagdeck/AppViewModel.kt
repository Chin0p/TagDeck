package com.tagdeck

import androidx.lifecycle.ViewModel
import com.tagdeck.data.DataRepository
import com.tagdeck.data.DefaultDataRepository

class AppViewModel : ViewModel() {
    val repository: DataRepository = DefaultDataRepository()
}
