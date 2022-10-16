package com.example.covid19gyeonsan.model

import com.google.firebase.firestore.GeoPoint

data class People(var idx : Int? = 0,
                  var no : Int? = 0,
                  var PersonalInfo : String? = null,
                  var meet : String? = null,
                  var hospital : String? = null,
                  var markerColor : Float = 330f,
                  var markerAlpha : Float = 0.7f,
                  var Region : String? = null,
                  var routes : ArrayList<RouteDTO> = arrayListOf(),
                  var Date : String? = null)

data class RouteDTO(var locate : GeoPoint? = null,
                    var title : String ?= null,
                    var idx : Int? = 0)