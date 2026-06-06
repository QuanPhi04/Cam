package com.example.cam

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONObject

class FruitStorage(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_FRUITS (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                image_uri TEXT,
                image_url TEXT,
                tags_json TEXT,
                is_ai_detected INTEGER NOT NULL,
                confidence REAL,
                scan_date TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FRUITS")
        onCreate(db)
    }

    fun getAllFruits(): List<Fruit> {
        val fruits = mutableListOf<Fruit>()
        readableDatabase.rawQuery(
            "SELECT * FROM $TABLE_FRUITS ORDER BY rowid DESC",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val imageUri = cursor.getString(cursor.getColumnIndexOrThrow("image_uri"))
                val imageUrl = cursor.getString(cursor.getColumnIndexOrThrow("image_url"))
                val confidenceIndex = cursor.getColumnIndexOrThrow("confidence")
                fruits += Fruit(
                    id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                    name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    imageUri = imageUri?.let(android.net.Uri::parse),
                    imageUrl = imageUrl,
                    tags = decodeTags(cursor.getString(cursor.getColumnIndexOrThrow("tags_json"))),
                    isAiDetected = cursor.getInt(cursor.getColumnIndexOrThrow("is_ai_detected")) == 1,
                    confidence = if (cursor.isNull(confidenceIndex)) null else cursor.getFloat(confidenceIndex),
                    scanDate = cursor.getString(cursor.getColumnIndexOrThrow("scan_date"))
                )
            }
        }
        return fruits
    }

    fun upsertFruit(fruit: Fruit) {
        val values = ContentValues().apply {
            put("id", fruit.id)
            put("name", fruit.name)
            put("image_uri", fruit.imageUri?.toString())
            put("image_url", fruit.imageUrl)
            put("tags_json", encodeTags(fruit.tags))
            put("is_ai_detected", if (fruit.isAiDetected) 1 else 0)
            put("confidence", fruit.confidence)
            put("scan_date", fruit.scanDate)
        }
        writableDatabase.insertWithOnConflict(
            TABLE_FRUITS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun deleteFruit(fruitId: String) {
        writableDatabase.delete(TABLE_FRUITS, "id = ?", arrayOf(fruitId))
    }

    fun clearAll() {
        writableDatabase.delete(TABLE_FRUITS, null, null)
    }

    private fun encodeTags(tags: Map<String, String>): String {
        val json = JSONObject()
        tags.forEach { (key, value) -> json.put(key, value) }
        return json.toString()
    }

    private fun decodeTags(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        val json = JSONObject(raw)
        return json.keys().asSequence().associateWith { key -> json.optString(key) }
    }

    companion object {
        private const val DATABASE_NAME = "FruitIDCam.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_FRUITS = "fruits"
    }
}
