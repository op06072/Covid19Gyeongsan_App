package com.example.covid19gyeonsan

import android.os.Bundle
import android.widget.TextView
import com.example.covid19gyeonsan.model.People
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class personal_info : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personal_info)
        val fireStore = FirebaseFirestore.getInstance()
        val idx = intent.getIntExtra("index", 0)
        val list = ArrayList<People>()
        findViewById<TextView>(R.id.idx)



        fireStore.collection("확진자동선").whereEqualTo("idx", idx).get()
            .addOnCompleteListener {
                if(it.isSuccessful){
                    for(dc in it.result!!.documents){
                        list.add(dc.toObject(People::class.java) as People)
                    }
                }
            }
    }
}