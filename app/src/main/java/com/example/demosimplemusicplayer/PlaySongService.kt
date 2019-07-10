package com.example.demosimplemusicplayer

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.util.Log
import android.view.View
import android.widget.RemoteViews

private const val SERVICE_TAG = "PLAY SONG SERVICE"
private const val CHANNEL_ID = "Channel ID"
private const val NOTIFICATION_ID = 211
private const val EXTRA_RAW = "RAW"

class PlaySongService : Service() {
	private val mBinder = LocalBinder()
	private lateinit var mMediaPlayer: MediaPlayer
	private lateinit var mNotification: Notification
	private lateinit var mRemoteView: RemoteViews
	private var mRaw: Int = 0

	inner class LocalBinder : Binder() {
		fun getServive(): PlaySongService = this@PlaySongService
	}

	override fun onBind(intent: Intent): IBinder {
		return mBinder
	}

	override fun onCreate() {
		super.onCreate()
		mMediaPlayer = MediaPlayer()
		Log.d(SERVICE_TAG, getString(R.string.msg_created))
	}

	override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
		Log.d(SERVICE_TAG, getString(R.string.msg_started))
		if (!mMediaPlayer.isPlaying) {
			when (intent.action) {
				Actions.CREATE.action -> {
					mRaw = intent.getIntExtra(EXTRA_RAW, 0)
					mMediaPlayer = MediaPlayer.create(applicationContext, mRaw)
				}
				Actions.PLAY_PAUSE.action -> {
					continueCurrent(getCurPosForSeekBar())
					mRemoteView.setViewVisibility(R.id.imgBtnPause, View.VISIBLE)
					mRemoteView.setViewVisibility(R.id.imgBtnPlay, View.GONE)
					startForeground(NOTIFICATION_ID, mNotification)
				}
			}
		} else {
			when (intent.action) {
				Actions.PLAY_PAUSE.action -> {
					pauseCurrent()
					mRemoteView.setViewVisibility(R.id.imgBtnPause, View.GONE)
					mRemoteView.setViewVisibility(R.id.imgBtnPlay, View.VISIBLE)
					startForeground(NOTIFICATION_ID, mNotification)
				}
				Actions.NEXT.action -> {
					continueCurrent(0)
					pauseCurrent()
					mRemoteView.setViewVisibility(R.id.imgBtnPause, View.GONE)
					mRemoteView.setViewVisibility(R.id.imgBtnPlay, View.VISIBLE)
					startForeground(NOTIFICATION_ID, mNotification)
				}
				Actions.PREVIOUS.action -> {
					continueCurrent(0)
					pauseCurrent()
					mRemoteView.setViewVisibility(R.id.imgBtnPause, View.GONE)
					mRemoteView.setViewVisibility(R.id.imgBtnPlay, View.VISIBLE)
					startForeground(NOTIFICATION_ID, mNotification)
				}
			}
		}
		return START_STICKY
	}

	override fun onUnbind(intent: Intent?): Boolean {
		return true
	}

	override fun onDestroy() {
		Log.d(SERVICE_TAG, getString(R.string.msg_destroyed))
		if (isSongPlaying()) mMediaPlayer.stop()
		mMediaPlayer.release()
		super.onDestroy()
	}

	private fun createNotification() {
		createNotificationChannel()
		mRemoteView = RemoteViews(packageName, R.layout.notification_layout)

		val notificationIntent = Intent(this, MainActivity::class.java)
				.apply {
			flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
		}
		val pendingIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)

		val pausePlayIntent = Intent(this, PlaySongService::class.java).apply {
			action = Actions.PLAY_PAUSE.action
		}
		val pendingPausePlay = PendingIntent.getService(this, 0, pausePlayIntent, 0)

		val nextIntent = Intent(this, PlaySongService::class.java).apply {
			action = Actions.NEXT.action
		}
		val pendingNext = PendingIntent.getService(this, 0, nextIntent, 0)

		val prevIntent = Intent(this, PlaySongService::class.java).apply {
			action = Actions.PREVIOUS.action
		}
		val pendingPrev = PendingIntent.getService(this, 0, prevIntent, 0)

		mRemoteView.setOnClickPendingIntent(R.id.imgBtnPlay, pendingPausePlay)
		mRemoteView.setOnClickPendingIntent(R.id.imgBtnPause, pendingPausePlay)
		mRemoteView.setOnClickPendingIntent(R.id.imgBtnNext, pendingNext)
		mRemoteView.setOnClickPendingIntent(R.id.imgBtnPrevious, pendingPrev)

		mRemoteView.setTextViewText(R.id.txtTitle, getString(R.string.title_song))

		mNotification = NotificationCompat.Builder(this, CHANNEL_ID)
			.setSmallIcon(R.mipmap.ic_launcher)
			.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
			.setStyle(NotificationCompat.DecoratedCustomViewStyle())
			.setContentIntent(pendingIntent)
			.setCustomContentView(mRemoteView)
			.build()

		startForeground(NOTIFICATION_ID, mNotification)
	}

	private fun createNotificationChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val name = getString(R.string.title_channel)
			val importance = NotificationManager.IMPORTANCE_DEFAULT
			val channel = NotificationChannel(CHANNEL_ID, name, importance)

			val notificationManager: NotificationManager =
				getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
			notificationManager.createNotificationChannel(channel)
		}
	}

	fun startSong() {
		mMediaPlayer.start()
		createNotification()
	}

	fun getCurPosForSeekBar(): Int = mMediaPlayer.currentPosition

	fun getDurationForSeekBar(): Int {
		if (mMediaPlayer.isPlaying)
			return mMediaPlayer.duration
		else return 0
	}

	fun pauseCurrent() {
		mMediaPlayer.pause()
	}

	fun isSongPlaying(): Boolean = mMediaPlayer.isPlaying

	fun continueCurrent(length: Int) {
		mMediaPlayer.seekTo(length)
		mMediaPlayer.start()
	}
}