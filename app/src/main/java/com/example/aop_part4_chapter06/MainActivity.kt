package com.example.aop_part4_chapter06

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.example.aop_part4_chapter06.databinding.ActivityMainBinding
import com.example.aop_part4_chapter06.service.AirKoreaApiService
import com.example.aop_part4_chapter06.service.KakaoApiService
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity(), CoroutineScope {

	private lateinit var binding: ActivityMainBinding
	private lateinit var mLocationManager: LocationManager
	private lateinit var mLocationListener: LocationListener
	private val job = Job()

	inner class myLocationListener : LocationListener {
		override fun onLocationChanged(location: Location) {
//            Log.i("LocationChanged", "${location.latitude} ${location.longitude}")
			//TODO: 좌표 변환
			convertLocationSystem(location)
			mLocationManager.removeUpdates(mLocationListener)
		}

		override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) = Unit

		override fun onProviderEnabled(p0: String?) = Unit

		override fun onProviderDisabled(p0: String?) = Unit
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityMainBinding.inflate(layoutInflater)
		setContentView(binding.root)

		initLocationManager()
		requestPermission()
	}

	override fun onDestroy() {
		super.onDestroy()
		mLocationManager.removeUpdates(mLocationListener)
	}

	private fun initLocationManager() {
		mLocationManager = getSystemService(LOCATION_SERVICE) as LocationManager
		mLocationListener = myLocationListener()
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
			Toast.makeText(this, "권한 허용", Toast.LENGTH_SHORT).show()
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

	override fun onRequestPermissionsResult(
		requestCode: Int,
		permissions: Array<out String>,
		grantResults: IntArray
	) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
		if (requestCode == REQUEST_CODE) {
			if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
				//TODO 권한 허용 -> 미세먼지 정보 표출
				setLocationListener()
				Toast.makeText(this, "권한 허용", Toast.LENGTH_SHORT).show()
			} else {
				//TODO: 권한 거부 사용자 교육 팝업 필요
				Toast.makeText(this, "권한 거부", Toast.LENGTH_SHORT).show()

				if (ActivityCompat.shouldShowRequestPermissionRationale(
						this,
						Manifest.permission.ACCESS_FINE_LOCATION
					)
				) {
					showRequestPermissionPopUp()
				}
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

	private fun convertLocationSystem(location: Location) {
		launch {
			withContext(Dispatchers.IO) {
				val response = RetrofitClient.getKaKaoRetrofit().create(KakaoApiService::class.java)
					.getTMSystemLocation(
						location.longitude,
						location.latitude,
						output_coord = "TM"
					)
				//TODO: response -> TM 좌표계로 변환된 값 (x : longitude, y : latitude)
				val response2 = RetrofitClient.getAirKoreaRetrofit().create(AirKoreaApiService::class.java)
					.getStationName(response.documents[0].x, response.documents[0].y)

				withContext(Dispatchers.Main) {
					Log.i("getStationName", response2.response.body.items.toString())
				}
			}
		}
	}


	override val coroutineContext: CoroutineContext
		get() = Dispatchers.Main + job

	companion object {
		const val REQUEST_CODE = 100
	}
}