package com.example.covid19gyeonsan

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.notice_list_item.view.*
import com.example.covid19gyeonsan.model.NewsDTO

class RecyclerViewAdapterNotice(private val items: List<NewsDTO>) : RecyclerView.Adapter<RecyclerViewAdapterNotice.NoticeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = NoticeViewHolder(parent)

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: NoticeViewHolder, position: Int) {
        items[position].let { item ->
            with(holder) {
                title.text = item.title.toString().replace("\\n","\n")
                time.text = item.time.toString().replace("\\n","\n")
                content.text = item.content.toString().replace("\\n","\n")
            }
        }
    }

    inner class NoticeViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.notice_list_item, parent, false)) {
        val title: TextView= itemView.text_title
        var time: TextView = itemView.text_time
        val content: TextView = itemView.text_content
    }

}
