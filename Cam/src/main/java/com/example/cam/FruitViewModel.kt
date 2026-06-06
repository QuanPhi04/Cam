package com.example.cam

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FruitViewModel(application: Application) : AndroidViewModel(application) {
    private val storage = FruitStorage(application)
    private val userRepository = UserRepository(application)
    private val preferences = application.getSharedPreferences("fruit_profile", Context.MODE_PRIVATE)

    private val _fruits = MutableLiveData(storage.getAllFruits())
    val fruits: LiveData<List<Fruit>> = _fruits

    private val _username = MutableLiveData(preferences.getString(KEY_USERNAME, "Bạn") ?: "Bạn")
    val username: LiveData<String> = _username

    private val _userProfile = MutableLiveData<UserProfile?>()
    val userProfile: LiveData<UserProfile?> = _userProfile

    val isFirebaseConfigured: Boolean
        get() = userRepository.isFirebaseConfigured

    init {
        val savedUsername = _username.value.orEmpty()
        if (savedUsername.isNotBlank() && savedUsername != "Bạn") {
            viewModelScope.launch {
                refreshRemoteHistory(savedUsername)
            }
        }
    }

    fun setUsername(name: String) {
        if (name.isNotBlank()) {
            _username.value = name
            preferences.edit().putString(KEY_USERNAME, name).apply()
        }
    }

    suspend fun userExists(name: String): Boolean = userRepository.userExists(name)

    suspend fun login(name: String, password: String): Boolean {
        val isValid = userRepository.login(name, password)
        if (isValid) {
            setUsername(name)
            refreshRemoteHistory(name)
        }
        return isValid
    }

    suspend fun register(name: String, password: String) {
        userRepository.register(name, password)
        setUsername(name)
        refreshRemoteHistory(name)
    }

    fun addFruit(fruit: Fruit) {
        val currentList = _fruits.value.orEmpty().toMutableList()
        val fruitToSave = fruit.withScanDateIfMissing()
        currentList.add(0, fruitToSave)
        storage.upsertFruit(fruitToSave)
        _fruits.value = currentList
        pushFruitToCloud(fruitToSave)
    }

    fun removeFruit(fruitId: String) {
        val currentList = _fruits.value.orEmpty().toMutableList()
        currentList.removeAll { it.id == fruitId }
        storage.deleteFruit(fruitId)
        _fruits.value = currentList
        val currentUsername = _username.value.orEmpty()
        if (currentUsername.isNotBlank() && currentUsername != "Bạn") {
            viewModelScope.launch {
                userRepository.deleteFruit(currentUsername, fruitId)
            }
        }
    }

    fun updateFruit(updatedFruit: Fruit) {
        val currentList = _fruits.value.orEmpty().toMutableList()
        val index = currentList.indexOfFirst { it.id == updatedFruit.id }
        if (index != -1) {
            val fruitToSave = updatedFruit.withScanDateIfMissing()
            currentList[index] = fruitToSave
            storage.upsertFruit(fruitToSave)
            _fruits.value = currentList
            pushFruitToCloud(fruitToSave)
        }
    }

    fun clearAllFruits() {
        storage.clearAll()
        _fruits.value = emptyList()
        val currentUsername = _username.value.orEmpty()
        if (currentUsername.isNotBlank() && currentUsername != "Bạn") {
            viewModelScope.launch {
                userRepository.clearHistory(currentUsername)
            }
        }
    }

    private suspend fun refreshRemoteHistory(name: String) {
        val remoteHistory = userRepository.loadHistory(name)
        storage.clearAll()
        remoteHistory.forEach(storage::upsertFruit)
        _fruits.value = remoteHistory
        
        // Also refresh profile
        val profile = userRepository.getProfile(name)
        _userProfile.value = profile
    }

    fun updateProfile(profile: UserProfile) {
        val currentUsername = _username.value.orEmpty()
        if (currentUsername.isNotBlank() && currentUsername != "Bạn") {
            _userProfile.value = profile
            viewModelScope.launch {
                userRepository.saveProfile(currentUsername, profile)
            }
        }
    }

    private fun pushFruitToCloud(fruit: Fruit) {
        val currentUsername = _username.value.orEmpty()
        if (currentUsername.isBlank() || currentUsername == "Bạn") return
        viewModelScope.launch {
            userRepository.saveFruit(currentUsername, fruit)
        }
    }

    fun logout() {
        _username.value = "Bạn"
        preferences.edit().remove(KEY_USERNAME).apply()
        storage.clearAll()
        _fruits.value = emptyList()
    }

    private fun Fruit.withScanDateIfMissing(): Fruit {
        if (scanDate.isNotBlank()) return this
        val currentDate = SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault()).format(Date())
        return copy(scanDate = currentDate)
    }

    companion object {
        private const val KEY_USERNAME = "username"
    }
}
