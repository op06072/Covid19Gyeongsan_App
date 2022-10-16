package com.example.covid19gyeonsan

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.covid19gyeonsan.model.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.android.synthetic.main.activity_maps.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.noButton
import org.jetbrains.anko.toast
import org.jetbrains.anko.yesButton
import org.json.JSONObject

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener {

    private val TAG = "MapsActivity"
    private lateinit var mMap: GoogleMap //마커, 카메라 지정을 위한 구글 맵 객체
    private lateinit var locationProvider : FusedLocationProviderClient // 위치 요청 메소드 담고 있는 객체
    private lateinit var locationRequest : LocationRequest // 위치 요청할 때 넘겨주는 데이터에 관한 객체
    private lateinit var locationCallback : MyLocationCallBack // 위치 확인되고 호출되는 객체
    private lateinit var mapView : View

    private var markers: ArrayList<Marker> = arrayListOf()
    private var peoples: ArrayList<People> = arrayListOf()
    private var masks: ArrayList<Mask> = arrayListOf()
    private var clinics: ArrayList<Clinic> = arrayListOf()
    var peoplemove : People? = null
    var maskplace : Mask? = null
    private var entirenum = 0

    private var spinnerList = ArrayList<String>()
    private val spinnerList2 = ArrayList<String>()
    private var peopleDetail = ArrayList<People>()

    private val REQUEST_ACCESS_FINE_LOCATION = 1000
    private val Patient = FirebaseFirestore.getInstance().collection("확진자동선").get()
    private val Clinics = FirebaseFirestore.getInstance().collection("선별진료소").get()
    var points = ArrayList<LatLng>()

    // 위치 정보를 얻기 위한 각종 초기화
    private fun locationInit() {
        // 이 객체의 메소드를 통해 위치 정보를 요청할 수 있음
        locationProvider = FusedLocationProviderClient(this)
        // 위치 갱신되면 호출되는 콜백 생성
        locationCallback = MyLocationCallBack()
        // (정보 요청할 때 넘겨줄 데이터)에 관한 객체 생성
        locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY // 가장 정확한 위치를 요청한다,
        //locationRequest.priority=LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY // 가장 정확한 위치를 요청한다,
        locationRequest.interval = 10000 // 위치를 갱신하는데 필요한 시간 <밀리초 기준>
        locationRequest.fastestInterval = 5000 // 다른 앱에서 위치를 갱신했을 때 그 정보를 가져오는 시간 <밀리초 기준>
    }

    // 위치 정보를 찾고 나서 인스턴스화되는 클래스
    inner class MyLocationCallBack : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            super.onLocationResult(locationResult)

            // lastLocation 프로퍼티가 가리키는 객체 주소를 받는다.
            // 그 객체는 현재 경도와 위도를 프로퍼티로 갖는다.
            // 그러나 gps 가 꺼져 있거나 위치를 찾을 수 없을 때는 lastLocation 은 null 을 가진다.
            val location = locationResult?.lastLocation
            //  gps가 켜져 있고 위치 정보를 찾을 수 있을 때 다음 함수를 호출한다. <?. : 안전한 호출>
            location?.run {
            }
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // 캐시/데이터 초기화 (데이터 잘못 입력했을 때 비정상 종료되는 문제 때문에)
        //clearApplicationData()

        // 화면이 꺼지지 않게 하기
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // 세로 모드로 화면 고정
        requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        /*showNoticeDialog()
            notice.setOnClickListener {
            showNoticeDialog()
        }*/

        setLocalPatients("전체")

        locationInit()

        // 프래그먼트 매니저로부터 SupportMapFragment프래그먼트를 얻는다. 이 프래그먼트는 지도를 준비하는 기능이 있다.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.main_map) as SupportMapFragment
        // 지도가 준비되면 알림을 받는다. (아마, get함수에서 다른 함수를 호출해서 처리하는 듯)
        mapFragment.getMapAsync(this)
        mapView = mapFragment.view!!
    }

    // 프로그램이 켜졌을 때만 위치 정보를 요청한다
    override fun onResume(){
        super.onResume()
        // 사용자에게 gps키라고 알리기
        //Toast.makeText(this,"이 앱은 GPS(위치)를 켜야 이용 가능합니다!", Toast.LENGTH_SHORT).show()
        // '앱이 gps사용'에 대한 권한이 있는지 체크
        // 거부됐으면 showPermissionInfoDialog(알림)메소드를 호출, 승인됐으면 addLocationListener(위치 요청)메소드를 호출

        permissionCheck(cancel={showPermissionInfoDialog()},
            ok={addLocationListener()})
    }
    // 프로그램이 중단되면 위치 요청을 삭제한다
    override fun onPause(){
        super.onPause()
        locationProvider.removeLocationUpdates(locationCallback)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        val customInfoWindow = CustomInfoWindowAdapter(this)

        mMap = googleMap

        // 현지 위치 이동 버튼 및 현재 위치 마커 표시
        try {
            mMap.isMyLocationEnabled = true
        } catch (e : Exception) { }

        mMap.uiSettings.isMyLocationButtonEnabled = true
        mMap.setOnInfoWindowClickListener(this)
        mMap.setInfoWindowAdapter(customInfoWindow)

        setLocalPatients("전체")
        MenuofActivity()

        // 확진자 경로 추적
        val latLng = LatLng(35.825060, 128.741459) // 경산시청
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f))

        buttonSetting()
    }

    // 위치 요청 메소드
    @SuppressLint("MissingPermission")
    private fun addLocationListener(){
        // 위치 정보 요청
        // (정보 요청할 때 넘겨줄 데이터)에 관한 객체, 위치 갱신되면 호출되는 콜백, 특정 스레드 지정(별 일 없으니 null)
        locationProvider.requestLocationUpdates(locationRequest,locationCallback,null)
        //toast("addLocationListener")
    }

    /**
     * 사용 가능한 맵을 조작한다.
     * 지도를 사용할 준비가 되면(알림을 받으면) 이 함수가 호출된다.
     * 이 함수에서 마커(표시)나 선을 추가하거나 카메라를 이동할 수 있다.
     * 기본 마커는 호주 시드니 근처이다.
     * GooglePlay 서비스가 기기에 설치돼있지 않은 경우,
     * 사용자에게 SupportMapFragment 안에 GooglePlay 서비스를 설치하라는 메시지가 표시된다.
     * 이 함수는 사용자가 GooglePlay 서비스를 설치하고 앱으로 돌아온 후에만 실행된다.
     **/
    private fun showMarker(menu : Int) {

        when (menu) {
            0 -> {

            }

            1 -> {
                val load = LoadingDialog(this@MapsActivity)
                load.show()
                spinner_people.visibility = View.INVISIBLE
                spinner_people2.visibility = View.INVISIBLE
                peoples.clear()
                spinnerList.clear()
                spinnerList2.clear()
                val arrayAdapter2 = ArrayAdapter(this, R.layout.custom_spinner, spinnerList2)
                arrayAdapter2.setDropDownViewResource(R.layout.custom_spinner_list)
                spinner_people2.adapter = arrayAdapter2
                val spinners = ArrayList<String>()
                spinnerList.add("전체")
                setLocalPatients("전체")
                Patient
                    .addOnSuccessListener { result ->
                        for (document in result) {
                            val p = People()
                            p.Region = document["거주지"] as String
                            p.PersonalInfo = document["인적사항"] as String
                            p.hospital = document["입원기관"] as String
                            p.idx = document.id.toInt()
                            val num = document["확진번호"].toString()
                            p.no = num.toInt()
                            p.meet = document["접촉자수(격리조치중)"] as String
                            p.Date = document["확진일자"] as String
                            for (R in document["이동경로"] as HashMap<String, HashMap<String, Any>>) {
                                val r = RouteDTO()
                                r.idx = R.key.toInt()
                                val Rv = R.value
                                r.title = Rv["경로"] as String
                                if ("좌표" in Rv) {
                                    r.locate = Rv["좌표"] as GeoPoint
                                    p.routes.add(r)
                                }
                            }
                            peoples.add(p)
                            if (!spinners.contains(p.Region as String) && p.routes.size != 0) spinners.add(p.Region as String)
                        }
                        val spinners2 = spinners.sorted()
                        spinners2.forEach{
                            spinnerList.add(it)
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.w(TAG, "Error getting documents.", exception)
                    }

                val arrayAdapter = ArrayAdapter(this, R.layout.custom_spinner, spinnerList)
                arrayAdapter.setDropDownViewResource(R.layout.custom_spinner_list)

                Handler().postDelayed({
                    spinner_people.adapter = arrayAdapter
                    spinner_people.visibility = View.VISIBLE
                    spinner_people.onItemSelectedListener = object : OnItemSelectedListener {
                        override fun onNothingSelected(parent: AdapterView<*>?) {

                        }

                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            removeAllMarker()
                            val localStr: String = spinner_people.selectedItem as String

                            setLocalPatients(localStr)

                            Handler().postDelayed({
                                viewSpinner2("갑제동")
                                Handler().postDelayed({
                                    viewSpinner2(localStr)
                                    spinner_people2.visibility = View.VISIBLE
                                },1)
                            }, 1)
                        }
                    }
                    // 콤보박스 선택에 따른 확진자 경로 표시 필터링
                    spinner_people2?.onItemSelectedListener = object : OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {

                            println("list size : ${peopleDetail.size}")

                            // 모든 마커/라인 삭제
                            removeAllMarker()
                            val bc = LatLngBounds.Builder()

                            peoplemove = null

                            for (depth2 in peopleDetail) {
                                addRoute2(depth2)
                            }

                            if (peoplemove != null) {
                                // 줌인 하기
                                for (i in points) {
                                    bc.include(i)
                                }
                                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bc.build(), 400))
                            }
                            if (spinner_people?.selectedItemPosition == 0) {
                                for (i in points) {
                                    bc.include(i)
                                }
                                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bc.build(), 200))
                            }
                            load.dismiss()
                        }
                        override fun onNothingSelected(parent: AdapterView<*>) {

                        }
                    }
                    load.dismiss()
                },1)

            }

            2 -> { // 마스크맵 용
                spinner_people.visibility = View.INVISIBLE
                spinner_people2.visibility = View.INVISIBLE
                val load = LoadingDialog(this@MapsActivity)
                setLocalPatients("전체")
                load.show()
                masks.clear()
                spinnerList.clear()
                val areas = listOf(
                    "하양읍", "진량읍", "압량읍", "와촌면", "자인면", "중방동", "사동", "삼풍동", "사정동",
                    "옥곡동", "옥산동", "중산동", "정평동", "삼북동", "백천동", "조영동", "대동"
                )
                spinnerList2.clear()
                val arealist = areas.sorted()
                val arrayAdapter = object : ArrayAdapter<String>(this, R.layout.custom_spinner){}
                arrayAdapter.add("전체")
                arrayAdapter.addAll(arealist)
                arrayAdapter.setDropDownViewResource(R.layout.custom_spinner_list)
                spinner_people.adapter = arrayAdapter
                spinner_people.visibility = View.VISIBLE
                spinner_people.onItemSelectedListener = object : OnItemSelectedListener {
                    override fun onNothingSelected(parent: AdapterView<*>?) {

                    }

                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        spinner_people2.visibility = View.INVISIBLE
                        load.show()
                        removeAllMarker()

                        viewSell(position)
                        spinner_people2.visibility = View.VISIBLE
                    }
                }

                // 콤보박스 선택에 따른 확진자 경로 표시 필터링
                spinner_people2?.onItemSelectedListener = object : OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                        println("list size : ${masks.size}")

                        // 모든 마커/라인 삭제
                        removeAllMarker()
                        val bc = LatLngBounds.Builder()

                        maskplace = null

                        if (spinner_people.selectedItemPosition == 0) {
                            Handler().postDelayed({
                                for (i in arealist){
                                    maskSell(i)
                                }
                            },1)
                        }
                        else {
                            maskSell(spinner_people.selectedItem as String)
                        }
                        //var latLng: LatLng?

                        if (maskplace != null) {
                            // 최초 마커 (격리 장소로 이동)
                            // 줌인 하기
                            for (i in points) {
                                bc.include(i)
                            }
                            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bc.build(), 400))
                        }
                        Handler().postDelayed({
                            if (spinner_people?.selectedItemPosition == 0) {
                                for (i in points) {
                                    bc.include(i)
                                }
                                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bc.build(), 400))
                            }
                        }, 1)
                        load.dismiss()
                    }
                    override fun onNothingSelected(parent: AdapterView<*>) {

                    }
                }
                Handler().postDelayed({
                    Handler().postDelayed({
                        Handler().postDelayed({
                            Handler().postDelayed({
                                Handler().postDelayed({
                                    load.dismiss()
                                }, 1)
                            }, 1)
                        }, 1)
                    }, 1)
                }, 1)
            }

            3 -> {
                spinner_people.visibility = View.INVISIBLE
                spinner_people2.visibility = View.INVISIBLE
                setLocalPatients("전체")
                spinnerList.clear()
                spinnerList2.clear()
                spinnerList.add("전체")
                Clinics
                    .addOnSuccessListener { result ->
                        for (document in result) {
                            val c = Clinic()
                            c.Name = document.id
                            c.phone = document["전화번호"] as String
                            c.Address = "경산시 "+document["주소"] as String
                            c.time = document["진료시간"] as String
                            c.ETC = document["비고"] as String
                            c.locate = document["좌표"] as GeoPoint
                            clinics.add(c)
                            spinnerList.add(c.Name as String)
                        }
                    }

                val arrayAdapter = ArrayAdapter(this, R.layout.custom_spinner, spinnerList)
                arrayAdapter.setDropDownViewResource(R.layout.custom_spinner_list)

                spinner_people.adapter = arrayAdapter
                spinner_people.visibility = View.VISIBLE
                spinner_people.onItemSelectedListener = object : OnItemSelectedListener {
                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        TODO("Not yet implemented")
                    }

                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        removeAllMarker()
                        val bc = LatLngBounds.Builder()
                        when(position) {
                            0 -> {
                                for (gps in clinics) {
                                    val snip = gps.Address+"\n"+gps.phone+"\n"+gps.time+"\n"+gps.ETC
                                    val latLng = LatLng(gps.locate!!.latitude, gps.locate!!.longitude)
                                    bc.include(latLng)

                                    // MarkerOptions 에 위치 정보를 넣을 수 있음
                                    val markerOptions = MarkerOptions()

                                    markerOptions.position(latLng)
                                    markerOptions.title(gps.Name)
                                    markerOptions.snippet(snip)
                                    markerOptions.alpha(gps.markerAlpha)
                                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(gps.markerColor))
                                    // 마커 추가
                                    val tMarker = mMap.addMarker(markerOptions)
                                    markers.add(tMarker)
                                }
                            }

                            else -> {
                                val gps = clinics[position-1]
                                val snip = gps.Address+"\n"+gps.phone+"\n"+gps.time+"\n"+gps.ETC
                                val latLng = LatLng(gps.locate!!.latitude, gps.locate!!.longitude)
                                bc.include(latLng)

                                // MarkerOptions 에 위치 정보를 넣을 수 있음
                                val markerOptions = MarkerOptions()

                                markerOptions.position(latLng)
                                markerOptions.title(gps.Name)
                                markerOptions.snippet(snip)
                                markerOptions.alpha(gps.markerAlpha)
                                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(gps.markerColor))
                                // 마커 추가
                                val tMarker = mMap.addMarker(markerOptions)
                                markers.add(tMarker)
                            }
                        }
                        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bc.build(), 400))
                    }

                }
            }

        }
    }

    private fun buttonSetting() {
        if (mapView.findViewById<View>(Integer.parseInt("1")) != null) {
            // Get the button view
            val locationButton =
                (mapView.findViewById<View>(Integer.parseInt("1")).parent as View).findViewById<View>(
                    Integer.parseInt("2")
                )
            // and next place it, on bottom right (as Google Maps app)
            val layoutParams = locationButton.layoutParams as RelativeLayout.LayoutParams
            // position on right bottom
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
            layoutParams.setMargins(0, 0, 30, 180)
        }
    }

    fun viewSell(pos : Int) {
        spinnerList2.clear()

        when (pos) {
            0 -> {
                spinnerList2.add("전체")
                spinnerList2.add("약국")
                spinnerList2.add("공적판매처")
            }

            else -> {
                val m = masks[pos-1]
                if (m.official.size != 0) {
                    if (m.pharmacy.size != 0) {
                        spinnerList2.add("전체")
                        spinnerList2.add("약국")
                    }
                    spinnerList2.add("공적판매처")
                }
                else spinnerList2.add("약국")
            }
        }

        val arrayAdapter2 = ArrayAdapter(this, R.layout.custom_spinner, spinnerList2)
        arrayAdapter2.setDropDownViewResource(R.layout.custom_spinner_list)
        spinner_people2.adapter = arrayAdapter2
    }

    fun viewSpinner2(localst : String) {
        spinnerList2.clear()
        spinnerList2.add("확진자")
        peopleDetail.clear()

        when(spinner_people?.selectedItemPosition) {
            0   -> {
                for (people in peoples) {
                    if (people.routes.size != 0){
                        spinnerList2.add("경산 ${people.idx}번 (${people.Region})")
                        peopleDetail.add(people)
                    }
                }
            }
            // 특정 확진자 한 명 선택 -> 해당 확진자의 이동 경로만 표시
            else -> {
                for (people in peoples) {
                    if (localst == people.Region && people.routes.size != 0) {
                        spinnerList2.add("경산 ${people.idx}번 (${people.Region})")
                        peopleDetail.add(people)
                    }
                }
            }
        }

        val arrayAdapter2 = ArrayAdapter(this, R.layout.custom_spinner, spinnerList2)
        arrayAdapter2.setDropDownViewResource(R.layout.custom_spinner_list)
        spinner_people2.adapter = arrayAdapter2
    }

    fun removeAllMarker() {
        // 모든 마커 삭제
        for(m in markers) {
            m.remove()}
        // 모든 라인 삭제
        markers.clear()
        // 모든 좌표 삭제
        points.clear()
    }

    @SuppressLint("SetTextI18n")
    private fun setLocalPatients(locate : String) {
        val db = FirebaseFirestore.getInstance().collection("발생동향")
        val patients = findViewById<TextView>(R.id.numOfPatients)
        val onCheck = findViewById<TextView>(R.id.onCheck)
        val negative = findViewById<TextView>(R.id.negative)
        val on = findViewById<TextView>(R.id.on)
        val cure = findViewById<TextView>(R.id.cure)
        val local_patients = findViewById<TextView>(R.id.local_patients)

        db.document("기본정보").get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    on.text = "("+"${document.get("갱신시간")})".substring(6)
                    onCheck.text = "검사중 ${document.get("검사중")}명"
                    negative.text = "음성 ${document.get("음성")}명"
                    cure.text = "완치 ${document.get("완치")}명"
                } else {
                    Log.d(TAG, "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
            }

        db.document("확진자수").get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val entire = "${document.get("전체")}"
                    entirenum = entire.toInt()
                    patients.text = " "+entire+"명"
                    local_patients.text = "$locate ${document.get(locate)}명"
                } else {
                    Log.d(TAG, "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
            }
    }

    private fun makemarker(ID : Int, option : MarkerOptions) {
        val bitmapdraw = getDrawable(ID) as BitmapDrawable
        val bitmap = bitmapdraw.bitmap
        val SmallMarker = Bitmap.createScaledBitmap(bitmap,200,200, false)
        option.icon(BitmapDescriptorFactory.fromBitmap(SmallMarker))
    }

    fun maskSell(i : String) {

        if (spinner_people2?.selectedItem.toString() == "전체") {
            if (masks.all { m: Mask -> m.Region != i}) {
                val maskapi = mapOf(
                    "하양읍" to "%ED%95%98%EC%96%91%EC%9D%8D",
                    "진량읍" to "%EC%A7%84%EB%9F%89%EC%9D%8D",
                    "압량읍" to "%EC%95%95%EB%9F%89%EC%9D%8D",
                    "와촌면" to "%EC%99%80%EC%B4%8C%EB%A9%B4",
                    "자인면" to "%EC%9E%90%EC%9D%B8%EB%A9%B4",
                    "중방동" to "%EC%A4%91%EB%B0%A9%EB%8F%99",
                    "사동" to "%EC%82%AC%EB%8F%99",
                    "삼풍동" to "%EC%82%BC%ED%92%8D%EB%8F%99",
                    "사정동" to "%EC%82%AC%EC%A0%95%EB%8F%99",
                    "옥곡동" to "%EC%98%A5%EA%B3%A1%EB%8F%99",
                    "옥산동" to "%EC%98%A5%EC%82%B0%EB%8F%99",
                    "중산동" to "%EC%A4%91%EC%82%B0%EB%8F%99",
                    "정평동" to "%EC%A0%95%ED%8F%89%EB%8F%99",
                    "삼북동" to "%EC%82%BC%EB%B6%81%EB%8F%99",
                    "백천동" to "%EB%B0%B1%EC%B2%9C%EB%8F%99",
                    "조영동" to "%EC%A1%B0%EC%98%81%EB%8F%99",
                    "대동" to "%EB%8C%80%EB%8F%99"
                )
                val html = "https://8oi9s0nnth.apigw.ntruss.com/corona19-masks/v1/storesByAddr/json?address=%EA%B2%BD%EC%83%81%EB%B6%81%EB%8F%84%20%EA%B2%BD%EC%82%B0%EC%8B%9C%20"+maskapi[i]
                val myJson = MaskJSON().execute(html).get() as JSONObject
                val jsonList = myJson.getJSONArray("stores")

                // MarkerOptions 에 위치 정보를 넣을 수 있음
                val markerOptions = MarkerOptions()
                val m = Mask()
                m.Region = i
                for(j in 0 until jsonList.length()) {
                    val gps = jsonList.getJSONObject(j)
                    if (gps.get("created_at").toString() != "null" && gps.getString("name") != "칠성약국") {
                        val child = Child()
                        child.Name = gps.getString("name")
                        child.Address = gps.getString("addr")
                        child.status = gps.getString("remain_stat")
                        child.update = gps.getString("created_at")
                        child.locate  = LatLng(gps.getDouble("lat"), gps.getDouble("lng"))

                        var snip = child.Name + "\n" + child.Address + "\n"
                        when(child.status) {
                            "break" -> {
                                snip += "판매중지"
                                makemarker(R.drawable.black_marker, markerOptions)
                            }
                            "empty" -> {
                                snip += "1개 이하"
                                makemarker(R.drawable.gray_marker, markerOptions)
                            }
                            "few" -> {
                                snip += "2개 이상 30개 미만"
                                makemarker(R.drawable.red_marker, markerOptions)
                            }
                            "some" -> {
                                snip += "30개 이상 100개 미만"
                                makemarker(R.drawable.yellow_marker, markerOptions)
                            }
                            "plenty" -> {
                                snip += "100개 이상"
                                makemarker(R.drawable.green_marker, markerOptions)
                            }
                        }
                        val updatetime = child.update as String
                        snip += "\n" + updatetime.substring(5, 16) + " 기준"

                        val latLng = child.locate as LatLng

                        points.add(latLng)

                        markerOptions.position(latLng)
                        markerOptions.title(gps.getString("name"))

                        markerOptions.snippet(snip)
                        markerOptions.alpha(0.7f)
                        // 마커 추가
                        val tMarker = mMap.addMarker(markerOptions)
                        markers.add(tMarker)
                        if (gps.getString("type") == "01") {
                            m.pharmacy.add(child)
                        }
                        else {
                            m.official.add(child)
                        }
                    }
                    maskplace = m
                }
                masks.add(m)
            }
            else {
                // MarkerOptions 에 위치 정보를 넣을 수 있음
                val markerOptions = MarkerOptions()
                maskplace = masks.find { m: Mask -> m.Region == i}
                for (gps in maskplace!!.pharmacy) {
                    var snip = gps.Name + "\n" + gps.Address + "\n"
                    when(gps.status) {
                        "break" -> {
                            snip += "판매중지"
                            makemarker(R.drawable.black_marker, markerOptions)
                        }
                        "empty" -> {
                            snip += "1개 이하"
                            makemarker(R.drawable.gray_marker, markerOptions)
                        }
                        "few" -> {
                            snip += "2개 이상 30개 미만"
                            makemarker(R.drawable.red_marker, markerOptions)
                        }
                        "some" -> {
                            snip += "30개 이상 100개 미만"
                            makemarker(R.drawable.yellow_marker, markerOptions)
                        }
                        "plenty" -> {
                            snip += "100개 이상"
                            makemarker(R.drawable.green_marker, markerOptions)
                        }
                    }
                    val updatetime = gps.update as String
                    snip += "\n" + updatetime.substring(5, 16) + " 기준"

                    val latLng = gps.locate as LatLng

                    points.add(latLng)

                    markerOptions.position(latLng)
                    markerOptions.title(gps.Name)

                    markerOptions.snippet(snip)
                    markerOptions.alpha(0.7f)
                    // 마커 추가
                    val tMarker = mMap.addMarker(markerOptions)
                    markers.add(tMarker)
                }
                for (gps in maskplace!!.official) {
                    var snip = gps.Name + "\n" + gps.Address + "\n"
                    when(gps.status) {
                        "break" -> {
                            snip += "판매중지"
                            makemarker(R.drawable.black_marker, markerOptions)
                        }
                        "empty" -> {
                            snip += "1개 이하"
                            makemarker(R.drawable.gray_marker, markerOptions)
                        }
                        "few" -> {
                            snip += "2개 이상 30개 미만"
                            makemarker(R.drawable.red_marker, markerOptions)
                        }
                        "some" -> {
                            snip += "30개 이상 100개 미만"
                            makemarker(R.drawable.yellow_marker, markerOptions)
                        }
                        "plenty" -> {
                            snip += "100개 이상"
                            makemarker(R.drawable.green_marker, markerOptions)
                        }
                    }
                    val updatetime = gps.update as String
                    snip += "\n" + updatetime.substring(5, 16) + " 기준"

                    val latLng = gps.locate as LatLng

                    points.add(latLng)

                    markerOptions.position(latLng)
                    markerOptions.title(gps.Name)

                    markerOptions.snippet(snip)
                    markerOptions.alpha(0.7f)
                    // 마커 추가
                    val tMarker = mMap.addMarker(markerOptions)
                    markers.add(tMarker)
                }
            }
        }
        else if (spinner_people2?.selectedItem.toString() == "약국") {
            // MarkerOptions 에 위치 정보를 넣을 수 있음
            val markerOptions = MarkerOptions()
            maskplace = masks.find { m: Mask -> m.Region == i}
            for (gps in maskplace!!.pharmacy) {
                var snip = gps.Name + "\n" + gps.Address + "\n"
                when(gps.status) {
                    "break" -> {
                        snip += "판매중지"
                        makemarker(R.drawable.black_marker, markerOptions)
                    }
                    "empty" -> {
                        snip += "1개 이하"
                        makemarker(R.drawable.gray_marker, markerOptions)
                    }
                    "few" -> {
                        snip += "2개 이상 30개 미만"
                        makemarker(R.drawable.red_marker, markerOptions)
                    }
                    "some" -> {
                        snip += "30개 이상 100개 미만"
                        makemarker(R.drawable.yellow_marker, markerOptions)
                    }
                    "plenty" -> {
                        snip += "100개 이상"
                        makemarker(R.drawable.green_marker, markerOptions)
                    }
                }
                val updatetime = gps.update as String
                snip += "\n" + updatetime.substring(5, 16) + " 기준"

                val latLng = gps.locate as LatLng

                points.add(latLng)

                markerOptions.position(latLng)
                markerOptions.title(gps.Name)

                markerOptions.snippet(snip)
                markerOptions.alpha(0.7f)
                // 마커 추가
                val tMarker = mMap.addMarker(markerOptions)
                markers.add(tMarker)
            }
        }
        else if (spinner_people2?.selectedItem.toString() == "공적판매처") {
            // MarkerOptions 에 위치 정보를 넣을 수 있음
            val markerOptions = MarkerOptions()
            maskplace = masks.find { m: Mask -> m.Region == i}
            for (gps in maskplace!!.official) {
                var snip = gps.Name + "\n" + gps.Address + "\n"
                when(gps.status) {
                    "break" -> {
                        snip += "판매중지"
                        makemarker(R.drawable.black_marker, markerOptions)
                    }
                    "empty" -> {
                        snip += "1개 이하"
                        makemarker(R.drawable.gray_marker, markerOptions)
                    }
                    "few" -> {
                        snip += "2개 이상 30개 미만"
                        makemarker(R.drawable.red_marker, markerOptions)
                    }
                    "some" -> {
                        snip += "30개 이상 100개 미만"
                        makemarker(R.drawable.yellow_marker, markerOptions)
                    }
                    "plenty" -> {
                        snip += "100개 이상"
                        makemarker(R.drawable.green_marker, markerOptions)
                    }
                }
                val updatetime = gps.update as String
                snip += "\n" + updatetime.substring(5, 16) + " 기준"

                val latLng = gps.locate as LatLng

                points.add(latLng)

                markerOptions.position(latLng)
                markerOptions.title(gps.Name)

                markerOptions.snippet(snip)
                markerOptions.alpha(0.7f)
                // 마커 추가
                val tMarker = mMap.addMarker(markerOptions)
                markers.add(tMarker)
            }
        }
    }

    fun addRoute2(patient: People) {
        // 첫 번째 콤보박스가 전국 이거나 선택한 항목일때만 출력
        if (spinner_people?.selectedItemPosition == 0 || spinner_people?.selectedItem.toString() == patient.Region) {
            when (spinner_people2?.selectedItemPosition) {
                0 -> {
                    addMarker2(patient)

                    if (spinner_people?.selectedItemPosition!! > 0 && patient.routes.size > 0 && peoplemove == null)  {
                        peoplemove = patient
                    }
                }
                // 특정 확진자 한 명 선택 -> 해당 확진자의 이동 경로만 표시
                else -> {
                    if ((spinner_people?.selectedItemPosition==0 || spinner_people?.selectedItem.toString() == patient.Region) &&
                        spinner_people2?.selectedItem.toString() == "경산 ${patient.idx}번 (${patient.Region})"
                    ) {

                        if (patient.routes.size > 0) {
                            addMarker2(patient)
                            peoplemove = patient
                        }
                    }
                }
            }
        }
    }

    private fun addMarker2(patient: People) {
        for(gps in patient.routes) {
            val latLng = LatLng(gps.locate!!.latitude, gps.locate!!.longitude)

            // MarkerOptions 에 위치 정보를 넣을 수 있음
            val markerOptions = MarkerOptions()

            points.add(latLng)

            markerOptions.position(latLng)
            markerOptions.title(gps.title)

            markerOptions.snippet(gps.title)
            markerOptions.alpha(patient.markerAlpha)
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(patient.markerColor))
            // 마커 추가
            val tMarker = mMap.addMarker(markerOptions)
            tMarker.tag = patient.idx
            markers.add(tMarker)

        }
    }

    private fun permissionCheck(cancel:()->Unit,ok:()->Unit){
        // 앱에 GPS이용하는 권한이 없을 때
        // <앱을 처음 실행하거나, 사용자가 이전에 권한 허용을 안 했을 때 성립>
        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED) {  // <PERMISSION_DENIED가 반환됨>
            // 이전에 사용자가 앱 권한을 허용하지 않았을 때 -> 왜 허용해야되는지 알람을 띄움
            showPermissionInfoDialog()
            // shouldShowRequestPermissionRationale메소드는 이전에 사용자가 앱 권한을 허용하지 않았을 때 ture를 반환함
        }
        // 앱에 권한이 허용됨
        else
            ok()
    }

    // 사용자가 권한 요청<허용,비허용>한 후에 이 메소드가 호출됨
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            // (gps 사용에 대한 사용자의 요청)일 때
            REQUEST_ACCESS_FINE_LOCATION->{
                // 요청이 허용일 때
                if(grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED)
                    addLocationListener()
                // 요청이 비허용일 때
                else{
                    toast("권한 거부 됨")
                    finish() }
            }
        }
    }

    // 사용자가 이전에 권한을 거부했을 때 호출된다.
    private fun showPermissionInfoDialog(){
        alert("지도 정보를 얻으려면 위치 권한이 필수로 필요합니다",""){
            yesButton{
                // 권한 요청
                ActivityCompat.requestPermissions(this@MapsActivity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),REQUEST_ACCESS_FINE_LOCATION)
            }
            noButton { toast("권한 거부 됨")
                finish() }
        }.show()
    }

    override fun onInfoWindowClick(p0: Marker?) {

    }

    private fun MenuofActivity() {
        val menus = arrayListOf("정보", "이동경로", "마스크맵", "진료소맵")
        showMarker(spinner_menu?.selectedItemPosition as Int)
        val arrayAdapter3 = ArrayAdapter(this, R.layout.custom_spinner, menus)
        arrayAdapter3.setDropDownViewResource(R.layout.custom_spinner_list)
        spinner_menu!!.adapter = arrayAdapter3
        val hint = ItemSpinnerMenu
        spinner_menu.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if ("정보" in menus){
                    when (position){
                        0 -> {
                            hint.visibility = View.VISIBLE
                            hint.bringToFront()
                        }

                        else -> {
                            menus.remove("정보")
                            arrayAdapter3.remove("정보")
                            hint.visibility = View.INVISIBLE
                            spinner_menu!!.adapter = arrayAdapter3
                            spinner_menu.setSelection(position-1)
                            showMarker(position)
                        }
                    }
                }
                else {
                    hint.visibility = View.INVISIBLE
                    showMarker(position+1)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }
        val arrayAdapter = ArrayAdapter(this, R.layout.custom_spinner, spinnerList)
        arrayAdapter.setDropDownViewResource(R.layout.custom_spinner_list)
        spinner_people.adapter = arrayAdapter
    }
}
