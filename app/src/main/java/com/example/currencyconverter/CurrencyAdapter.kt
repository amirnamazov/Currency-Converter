package com.example.currencyconverter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.row_layout.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL
import java.text.DecimalFormat
import java.util.logging.Handler
import kotlin.collections.ArrayList

class CurrencyAdapter(val context: Context, val values: ArrayList<Currency>) : RecyclerView.Adapter<CurrencyAdapter.ViewHolder>(){
    var parent:ViewGroup? = null

    //this method is returning the view for each item in the list
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CurrencyAdapter.ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.row_layout, parent, false)
        this.parent = parent

        return ViewHolder(v)
    }

    //this method is binding the data on the list
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        try {
            holder.bindItems(position)
            holder.textChanged(position, parent!![0].etInput)
            holder.getApiResult(position)
        }
        catch (e:Exception){ }
    }

    //this method is giving the size of the list
    override fun getItemCount(): Int {
        return values.size
    }

    //the class is holding the list view
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        @SuppressLint("WrongConstant")
        fun bindItems(position: Int) {

            itemView.tvCode.setText(values[position].code)

            if (position == 0) {
                itemView.tvRate.visibility = View.GONE
                itemView.tvOutput.visibility = View.GONE
                itemView.etInput.visibility = View.VISIBLE
            }

            else {
                itemView.tvRate.visibility = View.VISIBLE
                itemView.tvOutput.visibility = View.VISIBLE
                itemView.etInput.visibility = View.GONE

                itemView.setOnClickListener {
                    try {
                        val code = values[0].code
                        values[0].code = values[position].code
                        values[position].code = code

                        for (i in 0 until values.size){
                            values[i].rate = ""; values[i].outputValue = "" }

                        (context as Activity).recyclerView.adapter = CurrencyAdapter(context, values)
                    }
                    catch (e:Exception){ }
                }
            }
        }

        fun textChanged(position: Int, et:EditText){

            et.addTextChangedListener( object:TextWatcher{
                override fun afterTextChanged(s: Editable?) {
                    try {

                        if (s!!.isNotBlank() && s!!.isNotEmpty()){
                            if (position == 1){
                                for (i in 1 until values.size){
                                    val dec = DecimalFormat("#,###.####")
                                    val number = dec.format(s.toString().toFloat() * values[i].rate.toFloat())
                                    values[i].outputValue = number.toString()
                                }
                            }
                        }

                        else values.forEach { it.outputValue = "" }

                        itemView.tvOutput.setText(values[position].outputValue)
                    }
                    catch (e:Exception){ }
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }

        fun getApiResult(position:Int){

            // gets data from server when network available
            if (MainActivity.isNetworkAvailable(context)){

                (context as Activity).textView.visibility = View.GONE

                val code = values[0].code
                val API = "http://tayqatech.com/rates.php?base=$code"

                GlobalScope.launch(Dispatchers.IO){

                    try {
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

                        if (position == 1){
                            for (i in 1 until values.size){
                                val index = jsArray.indexOfObject("code", values[i].code)
                                values[i].rate = jsArray.getJSONObject(index!!).getString("rate")
                            }
                        }

                        withContext(Dispatchers.Main){
                            itemView.tvRate.setText("rate: " + values[position].rate)
                        }
                    }
                    catch (e: Exception){ }
                }
            }

            // gets data from local database when network not available
            else{
                (context as Activity).textView.visibility = View.VISIBLE

                fun getRateFromDB(code:String, fromTable:String):String{
                    var s:String = ""
                    try {
                        val dbLocal = LocalDB(context, null)
                        val cursor = dbLocal.getAllName(fromTable)

                        if (cursor!!.moveToFirst()){
                            cursor.moveToFirst()

                            if (cursor.getString(cursor.getColumnIndex(LocalDB.COLUMN_CODE)) == code)
                                s = cursor.getString(cursor.getColumnIndex(LocalDB.COLUMN_RATE))

                            else {
                                while (cursor.moveToNext()) {
                                    if (cursor.getString(cursor.getColumnIndex(LocalDB.COLUMN_CODE)) == code)
                                        s = cursor.getString(cursor.getColumnIndex(LocalDB.COLUMN_RATE))
                                }
                            }

                            cursor.close()
                        }
                    }
                    catch (e:Exception){}
                    return s
                }

                val codeTable = "table" + values[0].code
                for (i in 1 until values.size){
                    values[i].rate = getRateFromDB(values[i].code, codeTable)
                }

                itemView.tvRate.setText("rate: " + values[position].rate)
            }

            android.os.Handler().postDelayed({
                getApiResult(position)
            }, 5000)
        }
    }
}

class Currency(var code:String, var rate:String, var outputValue:String)