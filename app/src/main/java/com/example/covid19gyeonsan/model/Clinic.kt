package com.example.covid19gyeonsan.model

import com.google.firebase.firestore.GeoPoint

data class Clinic(var Name: String? = null,
                  var phone: String? = null,
                  var Address: String? = null,
                  var time: String? = null,
                  var ETC: String? = null,
                  var markerColor : Float = 50f,
                  var markerAlpha : Float = 0.7f,
                  var locate: GeoPoint? = null)