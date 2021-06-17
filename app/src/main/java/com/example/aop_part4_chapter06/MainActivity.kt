package com.example.aop_part4_chapter06

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.example.aop_part4_chapter06.databinding.ActivityMainBinding
import com.example.aop_part4_chapter06.response.AirKorea.AirKoreaData
import com.example.aop_part4_chapter06.response.AirKorea.AirQuality.Item
import com.example.aop_part4_chapter06.response.Kakao.Documents
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity(), CoroutineScope {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mLocationManager: LocationManager
    private lateinit var mLocationListener: LocationListener

    private val job = Job()

    inner class MyLocationListener : LocationListener {
        override fun onLocationChanged(location: Location) {
            //TODO: 대기오염 정보 획득
            fetchAirQualityData(location)
            ContextCompat.startForegroundService(
                this@MainActivity,
                Intent(this@MainActivity, WidgetProvider.UpdateWidgetService::class.java)
            )
            mLocationManager.removeUpdates(mLocationListener)
        }

        override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) = Unit

        override fun onProviderEnabled(provider: String) = Unit

        override fun onProviderDisabled(provider: String) = Unit
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initSwipeRefreshLayout()
        initLocationManager()
        requestPermission()
    }

    override fun onRestart() {
        super.onRestart()
        setLocationListener()
    }

    override fun onDestroy() {
        super.onDestroy()
        mLocationManager.removeUpdates(mLocationListener)
    }

    private fun initSwipeRefreshLayout() = with(binding) {
        mainRefreshLayout.setOnRefreshListener {
            binding.mainConstraintLayout.alpha = 0f
            binding.progressBar.isVisible = true
            binding.loadingTextView.isVisible = true
            setLocationListener()
        }
    }

    private fun initLocationManager() {
        mLocationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        mLocationListener = MyLocationListener()
    }

    @SuppressLint("MissingPermission")
    private fun setLocationListener() {
        mLocationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            0L,
            0f,
            mLocationListener
        )
        mLocationManager.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER,
            0L,
            0f,
            mLocationListener
        )
    }

    private fun requestPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            //TODO: 권한 허용 상태 -> 미세먼지 정보 표출
            setLocationListener()
        } else {
            //TODO: 권한 거부 상태 -> 권한 요청 work flow
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                REQUEST_CODE
            )
        }
    }

    private fun requestBackgroundPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION),
            BACKGROUND_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val locationPermissionGranted =
            requestCode == REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED

        val backgroundLocationPermissionGranted =
            requestCode == BACKGROUND_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (!backgroundLocationPermissionGranted) {
                requestBackgroundPermission()
            } else {
                setLocationListener()
            }
        } else {
            if (!locationPermissionGranted) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                ) {
                    showRequestPermissionPopUp()
                } else {
                    requestPermission()
                }
            } else {
                setLocationListener()
                Toast.makeText(this, "권한 허용", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showRequestPermissionPopUp() {
        val builder = AlertDialog.Builder(this)
            .setTitle("위치 정보 권한 요청")
            .setMessage("우리동네 미세먼지 측정 앱을 사용하기 위해서는 위치 권한 설정이 필요합니다.")
            .setPositiveButton("허용", object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ),
                        REQUEST_CODE
                    )
                }
            })
            .setNegativeButton("거부", object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    Toast.makeText(this@MainActivity, "권한을 거부 하였습니다.", Toast.LENGTH_SHORT).show()
                }
            })
            .create()

        builder.show()
    }

    private fun fetchAirQualityData(location: Location) {
        launch {
            withContext(Dispatchers.IO) {
                try {
                    val tmLocation = convertLocationSystem(location)
                    val station = getNearByMonitoringStation(tmLocation!!.x, tmLocation!!.y)
                    val data = station?.let { getData(it.stationName!!) }

                    withContext(Dispatchers.Main) {
                        displayData(data, station)
                    }
                } catch (e: Exception) {
                    Log.e("fetchAirQualityData", e.toString())
                } finally {
                    binding.progressBar.isVisible = false
                    binding.loadingTextView.isVisible = false
                    binding.mainRefreshLayout.isRefreshing = false
                }
            }
        }
    }

    //TODO: 좌표 변환
    private suspend fun convertLocationSystem(location: Location): Documents? {
        return RetrofitClient.kakaoAPI
            .getTMSystemLocation(
                location.longitude,
                location.latitude,
                output_coord = "TM"
            ).documents.firstOrNull()
    }

    //TODO: TM 좌표계로 변환된 값 (x : longitude, y : latitude)으로 주변 관측소 조회
    private suspend fun getNearByMonitoringStation(
        longitude: Double,
        latitude: Double
    ): AirKoreaData? {
        return RetrofitClient.airKoreaAPI
            .getStationName(longitude, latitude).response.body.items?.minByOrNull {
                it.tm!!
            }
    }

    //TODO: 가장 가까운 관측소에서 측정된 대기오염 정보 조회
    private suspend fun getData(stationName: String): Item? {
        return RetrofitClient.airKoreaAPI
            .getAirQualities(stationName).response?.body?.items?.firstOrNull()
    }

    //TODO: 데이터 바인딩
    @SuppressLint("SetTextI18n")
    private fun displayData(data: Item?, station: AirKoreaData?) = with(binding) {
        data?.let { item ->
            //TODO: item -> 대기오염 정보 / station -> 측정소 정보
            stationNameTextView.text = station?.stationName
            stationAddressTextView.text = "측정소 위치: ${station?.addr}"
            pm10ValueTextView.text = "${item.pm10Value} " + getString(R.string.pmUnit)
            pm25ValueTextView.text = "${item.pm25Value} " + getString(R.string.pmUnit)
            setKhaiGradeView(item)
            setSo2Data(item)
            setCoData(item)
            setO3Data(item)
            setNo2Data(item)
        }
        mainConstraintLayout.animate()
            .alpha(1f)
            .start()
    }

    private fun setKhaiGradeView(item: Item?) = with(binding) {
        when (item?.khaiGrade) {
            "1" -> {
                khaiGradeTextView.text = "좋음"
                Glide.with(khaiGradeImageView)
                    .load(R.drawable.grade_like)
                    .into(khaiGradeImageView)

                mainConstraintLayout.setBackgroundResource(R.color.background_grade_like)
            }
            "2" -> {
                khaiGradeTextView.text = "보통"
                Glide.with(khaiGradeImageView)
                    .load(R.drawable.grade_normal)
                    .into(khaiGradeImageView)

                mainConstraintLayout.setBackgroundResource(R.color.background_grade_normal)
            }
            "3" -> {
                khaiGradeTextView.text = "나쁨"
                Glide.with(khaiGradeImageView)
                    .load(R.drawable.grade_bad)
                    .into(khaiGradeImageView)

                mainConstraintLayout.setBackgroundResource(R.color.background_grade_bad)
            }
            "4" -> {
                khaiGradeTextView.text = "매우나쁨"
                Glide.with(khaiGradeImageView)
                    .load(R.drawable.grade_very_bad)
                    .into(khaiGradeImageView)

                mainConstraintLayout.setBackgroundResource(R.color.background_grade_very_bad)
            }
            else -> {
                khaiGradeTextView.text = "측정불가"
                Glide.with(khaiGradeImageView)
                    .load(R.drawable.grade_unknown)
                    .into(khaiGradeImageView)

                mainConstraintLayout.setBackgroundResource(R.color.background_grade_very_unknown)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setSo2Data(item: Item?) = with(binding) {
        so2Value.labelTextView.text = "아황산가스"
        so2Value.valueTextView.text = item?.so2Value + " ppm"
        when (item?.so2Grade) {
            "1" -> {
                so2Value.gradeTextView.text = getString(R.string.grade_like_text)
            }
            "2" -> {
                so2Value.gradeTextView.text = getString(R.string.grade_normal_text)
            }
            "3" -> {
                so2Value.gradeTextView.text = getString(R.string.grade_bad_text)
            }
            "4" -> {
                so2Value.gradeTextView.text = getString(R.string.grade_very_bad_text)
            }
            else -> {
                so2Value.gradeTextView.text = "측정 불가"
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setCoData(item: Item?) = with(binding) {
        coValue.labelTextView.text = "일산화탄소"
        coValue.valueTextView.text = item?.coValue + " ppm"
        when (item?.coGrade) {
            "1" -> {
                coValue.gradeTextView.text = getString(R.string.grade_like_text)
            }
            "2" -> {
                coValue.gradeTextView.text = getString(R.string.grade_normal_text)
            }
            "3" -> {
                coValue.gradeTextView.text = getString(R.string.grade_bad_text)
            }
            "4" -> {
                coValue.gradeTextView.text = getString(R.string.grade_very_bad_text)
            }
            else -> {
                coValue.gradeTextView.text = "측정 불가"
            }
        }
    }


    @SuppressLint("SetTextI18n")
    private fun setNo2Data(item: Item?) = with(binding) {
        no2Value.labelTextView.text = "이산화질소"
        no2Value.valueTextView.text = item?.no2Value + " ppm"
        when (item?.no2Grade) {
            "1" -> {
                no2Value.gradeTextView.text = getString(R.string.grade_like_text)
            }
            "2" -> {
                no2Value.gradeTextView.text = getString(R.string.grade_normal_text)
            }
            "3" -> {
                no2Value.gradeTextView.text = getString(R.string.grade_bad_text)
            }
            "4" -> {
                no2Value.gradeTextView.text = getString(R.string.grade_very_bad_text)
            }
            else -> {
                no2Value.gradeTextView.text = "측정 불가"
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setO3Data(item: Item?) = with(binding) {
        o3Value.labelTextView.text = "오존"
        o3Value.valueTextView.text = item?.o3Value + " ppm"
        when (item?.o3Grade) {
            "1" -> {
                o3Value.gradeTextView.text = getString(R.string.grade_like_text)
            }
            "2" -> {
                o3Value.gradeTextView.text = getString(R.string.grade_normal_text)
            }
            "3" -> {
                o3Value.gradeTextView.text = getString(R.string.grade_bad_text)
            }
            "4" -> {
                o3Value.gradeTextView.text = getString(R.string.grade_very_bad_text)
            }
            else -> {
                o3Value.gradeTextView.text = "측정 불가"
            }
        }
    }


    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    companion object {
        const val REQUEST_CODE = 100
        const val BACKGROUND_REQUEST_CODE = 101
    }
}