package com.example.demosimplemusicplayer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.SeekBar
import kotlinx.android.synthetic.main.activity_main.*

private const val MSG_HANDLER = 1
private const val MAIN_TAG = "MAIN"
private const val RAW = R.raw.tinh_cu_bao_gio_cung_tot_hon
private const val EXTRA_RAW = "RAW"

class MainActivity : AppCompatActivity() {

	private lateinit var mPlaySongService: PlaySongService
	private lateinit var mHandler: Handler
	private lateinit var mThread: Thread
	private var mIsBound: Boolean = true
	private var mPlayingTime = 0

	private val mConnection = object : ServiceConnection {
		override fun onServiceDisconnected(name: ComponentName?) {
			mIsBound = false
		}

		override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
			val binder = service as PlaySongService.LocalBinder
			mPlaySongService = binder.getServive()
			updateUI()
			mIsBound = true
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		Log.i(MAIN_TAG, getString(R.string.msg_created))

		mPlaySongService = PlaySongService()

		imgBtnPlaying.setOnClickListener {
			if (mPlayingTime == 0) {
				mPlaySongService.startSong()
				mPlayingTime++
			} else {
				mPlaySongService.continueCurrent(mPlaySongService.getCurPosForSeekBar())
			}
			getSeekBarStatus()
			pauseToPlay()
		}

		imgBtnPause.setOnClickListener {
			mPlaySongService.pauseCurrent()
			playToPause()
		}
		imgBtnNext.setOnClickListener {
			mPlaySongService.continueCurrent(0)
			mPlayingTime = 0
			playToPause()
		}
		imgBtnPrev.setOnClickListener {
			mPlaySongService.continueCurrent(0)
			mPlayingTime = 0
			playToPause()

		}
		seekBarPlaying.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
			override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
				Log.d(MAIN_TAG, getString(R.string.msg_seekbar_running))
			}

			override fun onStartTrackingTouch(seekBar: SeekBar?) {
				mPlaySongService.pauseCurrent()
				pauseToPlay()
			}

			override fun onStopTrackingTouch(seekBar: SeekBar) {
				val point = seekBar.progress
				mPlaySongService.continueCurrent(point)
				pauseToPlay()
			}
		})
	}

	override fun onStart() {
		super.onStart()
		Log.i(MAIN_TAG, getString(R.string.msg_started))
		val playSongIntent = Intent(this, PlaySongService::class.java)
		playSongIntent.apply {
			action = Actions.CREATE.action
			putExtra(EXTRA_RAW, RAW)
		}
		startService(playSongIntent)
		bindService(playSongIntent, mConnection, Context.BIND_AUTO_CREATE)
	}

	override fun onDestroy() {
		super.onDestroy()
		Log.i(MAIN_TAG, getString(R.string.msg_destroyed))
		val playSongIntent = Intent(this, PlaySongService::class.java)
		stopService(playSongIntent)
		unbindService(mConnection)
	}

	private fun getSeekBarStatus() {
		mThread = Thread(Runnable {
			val total = mPlaySongService.getDurationForSeekBar()
			seekBarPlaying.max = total
			var cur = mPlaySongService.getCurPosForSeekBar()
			while (cur < total) {
				val mMessage = Message()
				mMessage.apply {
					what = MSG_HANDLER
					arg1 = cur
					when (mPlaySongService.isSongPlaying()) {
						true -> arg2 = 1
						false -> arg2 = 0
					}
				}
				mHandler.sendMessage(mMessage)
				try {
					Thread.sleep(500)
					cur = mPlaySongService.getCurPosForSeekBar()
				} catch (e: InterruptedException) {
					e.printStackTrace()
				}
			}
		})
		mThread.start()
	}

	private fun updateUI() {
		mHandler = Handler(Looper.getMainLooper()) {
			when (it.what) {
				MSG_HANDLER -> {
					seekBarPlaying.progress = it.arg1
					if (it.arg2 == 1) {
						pauseToPlay()
					} else {
						playToPause()
					}
					return@Handler true
				}
				else -> return@Handler false
			}
		}
	}

	private fun pauseToPlay() {
		imgBtnPlaying.visibility = View.INVISIBLE
		imgBtnPause.visibility = View.VISIBLE
	}

	private fun playToPause() {
		imgBtnPlaying.visibility = View.VISIBLE
		imgBtnPause.visibility = View.INVISIBLE
	}
}
