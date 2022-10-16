package com.example.covid19gyeonsan

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.notice_dialog.*
import com.example.covid19gyeonsan.model.NewsDTO

class NoticeDialog(context: Context) : Dialog(context), View.OnClickListener {

    private var firestore : FirebaseFirestore? = null
    private val layout = R.layout.notice_dialog
    lateinit var recyclerView : RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout)

        recyclerview_notice.layoutManager = LinearLayoutManager(context)

        val news : ArrayList<NewsDTO> = arrayListOf()
        firestore = FirebaseFirestore.getInstance()
        firestore?.collection("notice")?.orderBy("time", Query.Direction.DESCENDING)?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
            news.clear()
            if(querySnapshot == null)return@addSnapshotListener

            // document 수만큼 획득
            for(snapshot in querySnapshot){
                val new = snapshot.toObject(NewsDTO::class.java)
                news.add(new)
                println("${new.title}, ${new.time}, ${new.content}")
            }

            recyclerview_notice.adapter = RecyclerViewAdapterNotice(news)
        }

        init()

    }

    private fun init() {
        //button_ok.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        /*when (v.id) {
            R.id.button_ok -> {
                dismiss()
            }
        }*/
    }
}