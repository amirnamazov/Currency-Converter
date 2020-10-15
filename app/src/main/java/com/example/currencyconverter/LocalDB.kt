package com.example.currencyconverter

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.lang.Exception
import java.net.URL

class LocalDB(context: Context, factory: SQLiteDatabase.CursorFactory?):
    SQLiteOpenHelper(context, DATABASE_NAME, factory, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        Codes.values().forEach {
            val CREATE_CURRENCY_TABLE = ("CREATE TABLE table" +
                    it.toString() + "(" +
                    COLUMN_ID + " INTEGER PRIMARY KEY," +
                    COLUMN_CODE + " TEXT," +
                    COLUMN_RATE + " TEXT" +
                    ")")

            db.execSQL(CREATE_CURRENCY_TABLE)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Codes.values().forEach {
            db.execSQL("DROP TABLE IF EXISTS table" + it.toString())
            onCreate(db)
        }
    }

    fun addName(tableName: String, rates: Rates) {
        val values = ContentValues()
        values.put(COLUMN_CODE, rates.code)
        values.put(COLUMN_RATE, rates.rate)
        val db = this.writableDatabase
        db.insert(tableName, null, values)
        db.close()
    }

    fun getAllName(tableName: String): Cursor? {
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM $tableName", null)
    }

    fun clearDB(tableName: String){
        val db = this.writableDatabase
        db.delete(tableName, null, null)
        db.close()
    }

    companion object {
        private val DATABASE_VERSION = 1
        private val DATABASE_NAME = "local.db"

        val COLUMN_ID = "_id"
        val COLUMN_CODE = "code"
        val COLUMN_RATE = "coefficient"
    }
}

class Rates(var code: String, var rate:String)