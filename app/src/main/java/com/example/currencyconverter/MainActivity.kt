package com.example.currencyconverter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils.indexOf
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.os.postDelayed
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.row_layout.view.*
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONStringer
import java.lang.*
import java.net.URL

class MainActivity : Activity() {

    @SuppressLint("WrongConstant")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            val values = ArrayList<Currency>()
            Codes.values().forEach { values.add(Currency(it.toString(), "", "")) }

            val adapter = CurrencyAdapter(this, values)
            recyclerView.layoutManager = LinearLayoutManager(this, LinearLayout.VERTICAL, false)
            recyclerView.adapter = adapter

            updateDB()


        }
        catch (e:Exception){ Toast.makeText(this, e.message, Toast.LENGTH_LONG).show() }
    }

    //updates local database
    fun updateDB(){

        for(i in 0 until Codes.values().size){

            if (isNetworkAvailable(this)){

                textView.visibility = View.GONE
                val codeTable = Codes.values()[i].toString()
                val API = "http://tayqatech.com/rates.php?base=$codeTable"

                GlobalScope.launch(Dispatchers.IO){
                    try {
                        val array = ArrayList<Rates>()
                        val codeTable = "table" + Codes.values()[i].toString()
                        val jsArray = JSONArray(URL(API).readText())

                        fun JSONArray.indexOfObject(name:String, value:String):Int?{
                            var index:Int? = null
                            for (i in 0 until this.length()){
                                if (this.getJSONObject(i).getString(name).equals(value) ||
                                    this.getJSONObject(i).getString(name) == value){
                                    index = i; break
                                }
                            }
                            return index
                        }

                        Codes.values().forEach {
                            try {
                                val index = jsArray.indexOfObject("code", it.toString())
                                val rate = jsArray.getJSONObject(index!!).getString("rate")
                                val user = Rates(it.toString(), rate)

                                array.add(user)
                            }
                            catch (e:Exception){  }
                        }

                        val dbLocal = LocalDB(this@MainActivity, null)
                        dbLocal.clearDB(codeTable)
                        array.forEach { dbLocal.addName(codeTable, it) }

                        withContext(Dispatchers.Main){

                            if (Codes.values().last() == Codes.values()[i]){

                                Handler().postDelayed({
                                    updateDB()
                                }, 10000)
                            }
                        }
                    }
                    catch (e: Exception){ }
                }
            }

            else{
                Handler().postDelayed({
                    textView.visibility = View.VISIBLE
                    updateDB()
                }, 5000)
                break
            }
        }
    }

    companion object{
        fun isNetworkAvailable(context: Context?): Boolean {
            if (context == null) return false
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                if (capabilities != null) {
                    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)){
                        return true
                    }
                }
            }

            else {
                val activeNetworkInfo = connectivityManager.activeNetworkInfo
                if (activeNetworkInfo != null && activeNetworkInfo.isConnected) {
                    return true
                }
            }

            return false
        }
    }
}