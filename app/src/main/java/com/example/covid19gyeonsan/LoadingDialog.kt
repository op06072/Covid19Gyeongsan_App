package com.example.covid19gyeonsan

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable

class LoadingDialog(context: MapsActivity) : Dialog(context){

    init {
        setCanceledOnTouchOutside(false)

        window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        setContentView(R.layout.dialog_loading)
    }
}