package com.example.aop_part4_chapter06

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.aop_part4_chapter06.service.AirKoreaApiService
import com.example.aop_part4_chapter06.service.KakaoApiService
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WidgetProvider : AppWidgetProvider() {

	//TODO: Service 설정해주어야 함
	// life-cycle dependency 추가

	override fun onUpdate(
		context: Context?,
		appWidgetManager: AppWidgetManager?,
		appWidgetIds: IntArray?
	) {
		super.onUpdate(context, appWidgetManager, appWidgetIds)

		ContextCompat.startForegroundService(
			context!!,
			Intent(context, UpdateWidgetService::class.java)
		)
	}


	class UpdateWidgetService : LifecycleService() {
		override fun onCreate() {
			super.onCreate()

			createChannelIfNeeded()

			startForeground(
				NOTIFICATION_ID,
				createNotification()
			)

		}

		override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

			if (ActivityCompat.checkSelfPermission(
					this,
					Manifest.permission.ACCESS_BACKGROUND_LOCATION
				) != PackageManager.PERMISSION_GRANTED
			) {
				val updateView = RemoteViews(packageName, R.layout.app_widget_simple_layout).apply {
					setViewVisibility(R.id.widgetProgressBar, View.VISIBLE)
					setViewVisibility(R.id.widgetContents, View.GONE)
				}
				updateWidget(updateView)
				stopSelf()
			}

			LocationServices.getFusedLocationProviderClient(this).lastLocation.addOnSuccessListener { location ->
				lifecycleScope.launch {
					withContext(Dispatchers.IO) {
						val tmLocation =
							RetrofitClient.kakaoAPI
								.getTMSystemLocation(
									location.longitude,
									location.latitude,
									"TM"
								).documents.firstOrNull()

						val station = RetrofitClient.airKoreaAPI.getStationName(
							tmX = tmLocation!!.x,
							tmY = tmLocation.y
						).response.body.items?.minByOrNull {
							it.tm!!
						}

						val item = RetrofitClient.airKoreaAPI.getAirQualities(
							station?.stationName!!
						).response?.body?.items?.firstOrNull()

						withContext(Dispatchers.Main) {
							val updateView = RemoteViews(
								packageName,
								R.layout.app_widget_simple_layout
							).apply {
								setViewVisibility(R.id.widgetContents, View.VISIBLE)
								setViewVisibility(R.id.widgetProgressBar, View.GONE)
								setTextViewText(
									R.id.pmValueTextView_widget,
									"${item?.pm10Value}" + getString(R.string.pmUnit)
								)
								setTextViewText(
									R.id.pmTitleTextView_widget,
									"미세먼지: ${station.stationName}"
								)
								when (item?.pm10Grade) {
									"1" -> {
										setImageViewResource(
											R.id.pmGradeImageView_widget,
											R.drawable.grade_like
										)
									}
									"2" -> {
										setImageViewResource(
											R.id.pmGradeImageView_widget,
											R.drawable.grade_normal
										)
									}
									"3" -> {
										setImageViewResource(
											R.id.pmGradeImageView_widget,
											R.drawable.grade_bad
										)
									}
									"4" -> {
										setImageViewResource(
											R.id.pmGradeImageView_widget,
											R.drawable.grade_very_bad
										)
									}
									else -> {
										setImageViewResource(
											R.id.pmGradeImageView_widget,
											R.drawable.grade_unknown
										)
									}
								}
							}
							updateWidget(updateView)
							stopSelf()
						}
					}
				}
			}

			return super.onStartCommand(intent, flags, startId)
		}

		override fun onDestroy() {
			super.onDestroy()
			stopForeground(true)
		}

		private fun createNotification() =
			NotificationCompat.Builder(this, WIDGET_REFRESH_CHANNEL_ID)
				.setSmallIcon(R.drawable.ic_baseline_refresh_24)
				.build()

		private fun createChannelIfNeeded() {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				(getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
					NotificationChannel(
						WIDGET_REFRESH_CHANNEL_ID,
						"위젯 갱신 채널",
						NotificationManager.IMPORTANCE_LOW
					)
				)
			}
		}

		private fun updateWidget(view: RemoteViews) {
			val provider = ComponentName(this, WidgetProvider::class.java)
			AppWidgetManager.getInstance(this).updateAppWidget(provider, view)
		}
	}

	companion object {
		const val NOTIFICATION_ID = 100
		const val WIDGET_REFRESH_CHANNEL_ID = "WIDGET_REFRESH_CHANNEL_ID"
	}
}