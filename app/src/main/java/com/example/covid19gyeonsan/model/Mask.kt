package com.example.covid19gyeonsan.model

import com.google.android.gms.maps.model.LatLng

data class Mask(var Region : String? = null,
                var pharmacy : ArrayList<Child> = arrayListOf(),
                var official : ArrayList<Child> = arrayListOf())

data class Child(var Name : String? = null,
                 var Address: String? = null,
                 var update: String? = null,
                 var status: String? = null,
                 var phone: String? = null,
                 var locate: LatLng? = null)