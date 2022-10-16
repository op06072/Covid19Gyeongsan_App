package com.example.covid19gyeonsan

import android.os.AsyncTask
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import kotlin.jvm.internal.Intrinsics

class MaskJSON :
    AsyncTask<String, Void?, JSONObject>(){

    override fun doInBackground(vararg str: String): JSONObject {
        //var readLine: String
        var myJson = JSONObject()
        Intrinsics.checkParameterIsNotNull(str, "params")
        try {
            for (url in str) {
                val bufferedReader = BufferedReader(
                    InputStreamReader(
                        URL(url).openStream(),"UTF-8"
                    )
                )
                val stringBuffer = StringBuffer()
                bufferedReader.forEachLine {
                    stringBuffer.append(it)
                }
                /*do {
                    readLine = bufferedReader.readLine()
                    stringBuffer.append(readLine)
                } while (readLine != null)*/
                bufferedReader.close()
                myJson = JSONObject(stringBuffer.toString())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return myJson
    }
    public override fun onPreExecute() {
        super.onPreExecute()
    }
}