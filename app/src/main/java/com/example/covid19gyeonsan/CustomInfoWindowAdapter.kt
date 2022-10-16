package com.example.covid19gyeonsan

import android.content.Context
import android.view.View
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import kotlinx.android.synthetic.main.custom_info_window.view.*
import org.jetbrains.anko.layoutInflater

class CustomInfoWindowAdapter(ctx : Context) : GoogleMap.InfoWindowAdapter {

    private var context : Context = ctx

    override fun getInfoContents(p0: Marker?): View? {
        val view = context.layoutInflater.inflate(R.layout.custom_info_window, null)

        view.text_title.text = p0?.title.toString()
        view.text_snippet.text = p0?.snippet.toString()

        return view
    }

    override fun getInfoWindow(p0: Marker?): View? {
        return null
    }
}