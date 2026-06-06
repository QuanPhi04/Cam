package com.example.cam

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.security.MessageDigest

class UserRepository(private val context: Context) {
    private val fallbackPrefs = context.getSharedPreferences("fruit_users_fallback", Context.MODE_PRIVATE)
    private val firestore: FirebaseFirestore?
        get() = if (FirebaseApp.getApps(context).isNotEmpty()) FirebaseFirestore.getInstance() else null

    val isFirebaseConfigured: Boolean
        get() = firestore != null

    suspend fun userExists(username: String): Boolean {
        val key = username.toUserKey()
        val db = firestore
        return if (db != null) {
            val snapshot = db.collection(USERS_COLLECTION).document(key).get().await()
            snapshot.exists() && snapshot.contains("passwordHash")
        } else {
            fallbackPrefs.contains("$key.passwordHash")
        }
    }

    suspend fun login(username: String, password: String): Boolean {
        val key = username.toUserKey()
        val passwordHash = hashPassword(key, password)
        val db = firestore
        return if (db != null) {
            val snapshot = db.collection(USERS_COLLECTION).document(key).get().await()
            val remoteHash = snapshot.getString("passwordHash")
            remoteHash != null && remoteHash == passwordHash
        } else {
            val localHash = fallbackPrefs.getString("$key.passwordHash", null)
            localHash != null && localHash == passwordHash
        }
    }

    suspend fun register(username: String, password: String) {
        val key = username.toUserKey()
        val passwordHash = hashPassword(key, password)
        val db = firestore
        if (db != null) {
            db.collection(USERS_COLLECTION).document(key)
                .set(
                    mapOf(
                        "username" to username.trim(),
                        "passwordHash" to passwordHash,
                        "createdAt" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                )
                .await()
        } else {
            fallbackPrefs.edit()
                .putString("$key.username", username.trim())
                .putString("$key.passwordHash", passwordHash)
                .apply()
        }
    }

    suspend fun loadHistory(username: String): List<Fruit> {
        val key = username.toUserKey()
        val db = firestore ?: return emptyList()
        val snapshot = db.collection(USERS_COLLECTION)
            .document(key)
            .collection(HISTORY_COLLECTION)
            .get()
            .await()
        return snapshot.documents.mapNotNull { document ->
            val data = document.data ?: return@mapNotNull null
            Fruit(
                id = document.id,
                name = data["name"] as? String ?: return@mapNotNull null,
                imageUri = (data["imageUri"] as? String)?.let(android.net.Uri::parse),
                imageUrl = data["imageUrl"] as? String,
                tags = (data["tags"] as? Map<*, *>)
                    ?.mapNotNull { (key, value) ->
                        val tagKey = key as? String ?: return@mapNotNull null
                        tagKey to value.toString()
                    }
                    ?.toMap()
                    .orEmpty(),
                isAiDetected = data["isAiDetected"] as? Boolean ?: false,
                confidence = (data["confidence"] as? Number)?.toFloat(),
                scanDate = data["scanDate"] as? String ?: ""
            )
        }.sortedByDescending { it.scanDate }
    }

    suspend fun saveFruit(username: String, fruit: Fruit) {
        val db = firestore ?: return
        db.collection(USERS_COLLECTION)
            .document(username.toUserKey())
            .collection(HISTORY_COLLECTION)
            .document(fruit.id)
            .set(fruit.toRemoteMap(), SetOptions.merge())
            .await()
    }

    suspend fun deleteFruit(username: String, fruitId: String) {
        val db = firestore ?: return
        db.collection(USERS_COLLECTION)
            .document(username.toUserKey())
            .collection(HISTORY_COLLECTION)
            .document(fruitId)
            .delete()
            .await()
    }

    suspend fun clearHistory(username: String) {
        val db = firestore ?: return
        val history = db.collection(USERS_COLLECTION)
            .document(username.toUserKey())
            .collection(HISTORY_COLLECTION)
            .get()
            .await()
        history.documents.forEach { it.reference.delete().await() }
    }

    private fun Fruit.toRemoteMap(): Map<String, Any?> {
        return mapOf(
            "name" to name,
            "imageUri" to imageUri?.toString(),
            "imageUrl" to imageUrl,
            "tags" to tags,
            "tagsJson" to JSONObject(tags).toString(),
            "isAiDetected" to isAiDetected,
            "confidence" to confidence,
            "scanDate" to scanDate,
            "updatedAt" to System.currentTimeMillis()
        )
    }

    suspend fun saveProfile(username: String, profile: UserProfile) {
        val key = username.toUserKey()
        val db = firestore
        if (db != null) {
            db.collection(USERS_COLLECTION).document(key)
                .set(profile.toMap(), SetOptions.merge())
                .await()
        } else {
            fallbackPrefs.edit().apply {
                putString("$key.name", profile.name)
                putInt("$key.age", profile.age ?: -1)
                putFloat("$key.weight", profile.weight ?: -1f)
                putString("$key.dob", profile.dob)
                putString("$key.hobbies", profile.hobbies)
                putString("$key.diet", profile.diet)
                putString("$key.avatarUrl", profile.avatarUrl)
            }.apply()
        }
    }

    suspend fun getProfile(username: String): UserProfile? {
        val key = username.toUserKey()
        val db = firestore
        return if (db != null) {
            val snapshot = db.collection(USERS_COLLECTION).document(key).get().await()
            if (snapshot.exists()) {
                UserProfile(
                    name = snapshot.getString("name"),
                    age = (snapshot.get("age") as? Number)?.toInt(),
                    weight = (snapshot.get("weight") as? Number)?.toFloat(),
                    dob = snapshot.getString("dob"),
                    hobbies = snapshot.getString("hobbies"),
                    diet = snapshot.getString("diet"),
                    avatarUrl = snapshot.getString("avatarUrl")
                )
            } else null
        } else {
            if (fallbackPrefs.contains("$key.username")) {
                UserProfile(
                    name = fallbackPrefs.getString("$key.name", null),
                    age = fallbackPrefs.getInt("$key.age", -1).takeIf { it != -1 },
                    weight = fallbackPrefs.getFloat("$key.weight", -1f).takeIf { it != -1f },
                    dob = fallbackPrefs.getString("$key.dob", null),
                    hobbies = fallbackPrefs.getString("$key.hobbies", null),
                    diet = fallbackPrefs.getString("$key.diet", null),
                    avatarUrl = fallbackPrefs.getString("$key.avatarUrl", null)
                )
            } else null
        }
    }

    private fun UserProfile.toMap(): Map<String, Any?> {
        return mapOf(
            "name" to name,
            "age" to age,
            "weight" to weight,
            "dob" to dob,
            "hobbies" to hobbies,
            "diet" to diet,
            "avatarUrl" to avatarUrl,
            "updatedAt" to System.currentTimeMillis()
        )
    }

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val HISTORY_COLLECTION = "history"

        fun String.toUserKey(): String = trim().lowercase()
            .replace(Regex("[^a-z0-9_-]"), "_")
            .ifBlank { "user" }

        private fun hashPassword(usernameKey: String, password: String): String {
            val bytes = MessageDigest.getInstance("SHA-256")
                .digest("$usernameKey:$password".toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}

data class UserProfile(
    val name: String? = null,
    val age: Int? = null,
    val weight: Float? = null,
    val dob: String? = null,
    val hobbies: String? = null,
    val diet: String? = null,
    val avatarUrl: String? = null
)
