package com.example.covid19gyeonsan.model

data class NoticeDTO (var statistics : String? = null) {
}

data class NewsDTO (var title : String? = null,
                    var time : String? = null,
                    var content : String? = null)