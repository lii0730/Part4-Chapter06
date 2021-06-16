package com.example.aop_part4_chapter06

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.RemoteViews
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.aop_part4_chapter06.response.AirKorea.AirKoreaData
import com.example.aop_part4_chapter06.response.AirKorea.AirQuality.Item
import com.example.aop_part4_chapter06.response.Kakao.Documents
import com.example.aop_part4_chapter06.service.AirKoreaApiService
import com.example.aop_part4_chapter06.service.KakaoApiService
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnSuccessListener
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class WidgetProvider : AppWidgetProvider(), CoroutineScope {

	private val job = Job()
	private lateinit var mLocationManager: LocationManager
	private lateinit var mLocationListener: LocationListener
	private lateinit var currentLocation : Location

	inner class UpdateLocation : LocationListener {
		override fun onLocationChanged(location: Location?) {
			currentLocation = location!!
		}

		override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

		override fun onProviderEnabled(provider: String?) = Unit

		override fun onProviderDisabled(provider: String?) = Unit
	}

	private fun initLocationManager(context: Context?) {
		mLocationManager =  context!!.getSystemService(AppCompatActivity.LOCATION_SERVICE) as LocationManager
		mLocationListener = UpdateLocation()
	}

	@SuppressLint("MissingPermission")
	private fun setLocationListener() {
		mLocationManager.requestLocationUpdates(
			LocationManager.NETWORK_PROVIDER,
			0L,
			0f,
			mLocationListener
		)
		mLocationManager.requestLocationUpdates(
			LocationManager.GPS_PROVIDER,
			0L,
			0f,
			mLocationListener
		)
	}

	override fun onUpdate(
		context: Context?,
		appWidgetManager: AppWidgetManager?,
		appWidgetIds: IntArray?
	) {
		super.onUpdate(context, appWidgetManager, appWidgetIds)

		initLocationManager(context)
		setLocationListener()

		appWidgetIds?.forEach { appWidgetId ->

			val pendingIntent: PendingIntent =
				Intent(context, MainActivity::class.java).let { intent ->
					PendingIntent.getActivity(context, 0, intent, 0)
				}

			val view: RemoteViews =
				RemoteViews(context!!.packageName, R.layout.app_widget_simple_layout).apply {
					setOnClickPendingIntent(R.id.widget_layout, pendingIntent)
				}

			updateView(view, context)

			appWidgetManager?.updateAppWidget(appWidgetId, view)
		}
	}

	private fun updateView(view: RemoteViews, context: Context?) {
		if (ActivityCompat.checkSelfPermission(
				context!!,
				Manifest.permission.ACCESS_FINE_LOCATION
			) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
				context,
				Manifest.permission.ACCESS_COARSE_LOCATION
			) == PackageManager.PERMISSION_GRANTED
		) {
			launch {
				withContext(Dispatchers.IO) {
					//TODO: DB에 실시간 대기오염 정보 저장 후 갱신하면서 사용하는 방법도 고려중
					val tmLocation = convertLocationSystem()
					val station =
						getNearByMonitoringStation(tmLocation!!.x, tmLocation!!.y)
					val data = station?.let { getData(it.stationName!!) }
					withContext(Dispatchers.Main) {
						setImageResource(view, data)
						view.setTextViewText(
							R.id.pmValueTextView_widget,
							data?.pm10Value
						)
						view.setTextViewText(R.id.pmTitleTextView_widget, "미세먼지")
					}
				}
			}
		}
	}

	private suspend fun convertLocationSystem(): Documents? {
		return RetrofitClient.getKaKaoRetrofit().create(KakaoApiService::class.java)
			.getTMSystemLocation(currentLocation.longitude, currentLocation.latitude, "TM")
			.documents
			.firstOrNull()
	}

	private suspend fun getNearByMonitoringStation(
		longitude: Double,
		latitude: Double
	): AirKoreaData? {
		return RetrofitClient.getAirKoreaRetrofit().create(AirKoreaApiService::class.java)
			.getStationName(longitude, latitude).response.body.items?.minByOrNull {
				it.tm!!
			}
	}

	private suspend fun getData(stationName: String): Item? {
		return RetrofitClient.getAirKoreaRetrofit().create(AirKoreaApiService::class.java)
			.getAirQualities(stationName).response?.body?.items?.firstOrNull()
	}

	private fun setImageResource(view: RemoteViews, data: Item?) {
		when (data?.pm10Grade) {
			"1" -> {
				view.setImageViewResource(R.id.pmGradeImageView_widget, R.drawable.grade_like)
			}
			"2" -> {
				view.setImageViewResource(R.id.pmGradeImageView_widget, R.drawable.grade_normal)
			}
			"3" -> {
				view.setImageViewResource(R.id.pmGradeImageView_widget, R.drawable.grade_bad)
			}
			"4" -> {
				view.setImageViewResource(
					R.id.pmGradeImageView_widget,
					R.drawable.grade_very_bad
				)
			}
			else -> {
				view.setImageViewResource(
					R.id.pmGradeImageView_widget,
					R.drawable.grade_unknown
				)
			}
		}
	}
	override val coroutineContext: CoroutineContext
		get() = Dispatchers.Main + job
}