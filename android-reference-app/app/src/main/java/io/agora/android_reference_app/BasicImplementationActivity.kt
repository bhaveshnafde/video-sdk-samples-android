package io.agora.android_reference_app

import androidx.appcompat.app.AppCompatActivity
import io.agora.agora_manager.AgoraManager
import android.view.SurfaceView
import android.os.Bundle
import android.view.View
import android.widget.*
import io.agora.agora_manager.AgoraManager.ProductName
import io.agora.agora_manager.AgoraManager.AgoraManagerListener
import java.util.HashMap

open class BasicImplementationActivity : AppCompatActivity() {
    protected var agoraManager: AgoraManager? = null
    private var baseLayout: LinearLayout? = null
    protected var btnJoinLeave: Button? = null
    protected var mainFrame: FrameLayout? = null
    protected var containerLayout: LinearLayout? = null
    protected var radioGroup: RadioGroup? = null
    protected var videoFrameMap: MutableMap<Int, FrameLayout?>? = null
    protected var surfaceViewMain: SurfaceView? = null

    protected open fun initializeAgoraManager() {
        agoraManager = AgoraManager(this)
        // Set up a listener for updating the UI
        agoraManager!!.setListener(agoraManagerListener)
    }

    // Default layout resource ID for base activity
    protected open val layoutResourceId: Int
        get() = R.layout.activity_basic_implementation // Default layout resource ID for base activity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layoutResourceId)

        // Find the root view of the included layout
        baseLayout = findViewById(R.id.base_layout)
        // Find the widgets inside the included layout using the root view
        btnJoinLeave = baseLayout!!.findViewById(R.id.btnJoinLeave)
        // Find the main video frame
        mainFrame = findViewById(R.id.main_video_container)
        // Find the multi video container layout
        containerLayout = findViewById(R.id.containerLayout)
        // Find the radio group for role
        radioGroup = findViewById(R.id.radioGroup)
        videoFrameMap = HashMap()

        // Create an instance of the AgoraManager class
        initializeAgoraManager()

        // Set the Agora product
        val intent = intent
        if (intent != null) {
            val intValue = intent.getIntExtra("selectedProduct", 0)
            val selectedProduct = ProductName.values()[intValue]
            agoraManager!!.currentProduct = selectedProduct
        }
        if (agoraManager!!.currentProduct === ProductName.INTERACTIVE_LIVE_STREAMING
            || agoraManager!!.currentProduct === ProductName.BROADCAST_STREAMING
        ) {
            radioGroup!!.visibility = View.VISIBLE
            // Hide the horizontal scrolling video view
            findViewById<View>(R.id.smallVideosView).visibility = View.GONE
        } else {
            radioGroup!!.visibility = View.GONE
        }
        radioGroup!!.setOnCheckedChangeListener { _: RadioGroup?, checkedId: Int ->
            agoraManager!!.setBroadcasterRole(
                checkedId == R.id.radioButtonBroadcaster
            )
        }
    }

    protected open fun join() {
        agoraManager!!.joinChannel()
    }

    protected fun showLocalVideo() {
        if (agoraManager!!.isBroadcaster) {
            runOnUiThread {

                // Get the SurfaceView for the local video
                val localVideoSurfaceView = agoraManager!!.localVideo
                // Add te SurfaceView to a FrameLayout
                mainFrame!!.addView(localVideoSurfaceView)
                surfaceViewMain = localVideoSurfaceView
                // Associate the FrameLayout
                videoFrameMap!![agoraManager!!.localUid] = mainFrame
                mainFrame!!.tag = agoraManager!!.localUid
            }
        }
    }

    protected open fun leave() {
        agoraManager!!.leaveChannel()
        btnJoinLeave!!.text = getString(R.string.join)
        if (radioGroup!!.visibility != View.GONE) radioGroup!!.visibility = View.VISIBLE

        // Clear the video containers
        containerLayout!!.removeAllViews()
        mainFrame!!.removeAllViews()
        videoFrameMap!!.clear()
    }

    fun joinLeave(view: View) {
        if (!agoraManager!!.isJoined) {
            join()
        } else {
            leave()
        }
    }

    protected fun showMessage(message: String?) {
        runOnUiThread { Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show() }
    }// Remove the FrameLayout from the LinearLayout// Get the FrameLayout in which the video was displayed

    // If the video was in the main frame swap it with the local frame
// Use the main frame

    // Add the SurfaceView to the FrameLayout
    // Associate the remoteUid with the FrameLayout for use in swapping
    // Create a new FrameLayout
    // Set an onclick listener for video swapping
    // Set the layout parameters for the new FrameLayout
    protected val agoraManagerListener: AgoraManagerListener
        get() = object : AgoraManagerListener {
            override fun onMessageReceived(message: String?) {
                showMessage(message)
            }

            override fun onRemoteUserJoined(remoteUid: Int, surfaceView: SurfaceView?) {
                runOnUiThread {
                    val targetLayout: FrameLayout?
                    if (agoraManager!!.currentProduct === ProductName.VIDEO_CALLING) {
                        // Create a new FrameLayout
                        targetLayout = FrameLayout(applicationContext)
                        // Set an onclick listener for video swapping
                        targetLayout.setOnClickListener(videoClickListener)
                        // Set the layout parameters for the new FrameLayout
                        val layoutParams = LinearLayout.LayoutParams(
                            400,
                            LinearLayout.LayoutParams.MATCH_PARENT
                        )
                        layoutParams.setMargins(6, 6, 6, 6)
                        // Set the id for the new FrameLayout
                        targetLayout.id = View.generateViewId()
                        // Add the new FrameLayout to the parent LinearLayout
                        containerLayout!!.addView(targetLayout, layoutParams)
                    } else if (!agoraManager!!.isBroadcaster) {
                        // Use the main frame
                        targetLayout = mainFrame
                        surfaceViewMain = surfaceView
                    } else {
                        return@runOnUiThread
                    }

                    // Add the SurfaceView to the FrameLayout
                    targetLayout!!.addView(surfaceView)
                    // Associate the remoteUid with the FrameLayout for use in swapping
                    targetLayout.tag = remoteUid
                    videoFrameMap!![remoteUid] = targetLayout
                }
            }

            override fun onRemoteUserLeft(remoteUid: Int) {
                runOnUiThread {

                    // Get the FrameLayout in which the video was displayed
                    val frameLayoutOfUser = videoFrameMap!![remoteUid]

                    // If the video was in the main frame swap it with the local frame
                    if (frameLayoutOfUser!!.id == mainFrame!!.id) {
                        if (agoraManager!!.currentProduct === ProductName.VIDEO_CALLING) {
                            swapVideo(videoFrameMap!![agoraManager!!.localUid]!!.id)
                            // Remove the FrameLayout from the LinearLayout
                            val frameLayoutToDelete = videoFrameMap!![remoteUid]
                            containerLayout!!.removeView(frameLayoutToDelete)
                        } else {
                            mainFrame!!.removeView(surfaceViewMain)
                        }
                    }
                    videoFrameMap!!.remove(remoteUid)
                }
            }

            override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                runOnUiThread {
                    btnJoinLeave!!.text = getString(R.string.leave)
                    showLocalVideo()
                    if (radioGroup!!.visibility != View.GONE) radioGroup!!.visibility =
                        View.INVISIBLE
                }
            }
        }

    // A small video frame was clicked
    protected val videoClickListener: View.OnClickListener
        get() = View.OnClickListener { v: View ->
            // A small video frame was clicked
            swapVideo(v.id)
        }

    protected open fun swapVideo(frameId: Int) {
        // Swap the  video in the small frame with the main frame
        runOnUiThread {

            // Swap the videos in the small frame and the main frame
            val smallFrame = findViewById<FrameLayout>(frameId)

            // Get the SurfaceView in the small frame
            val surfaceViewSmall = smallFrame.getChildAt(0) as SurfaceView

            // Swap the SurfaceViews
            mainFrame!!.removeView(surfaceViewMain)
            smallFrame.removeView(surfaceViewSmall)
            mainFrame!!.addView(surfaceViewSmall, 0)
            smallFrame.addView(surfaceViewMain)
            surfaceViewMain = surfaceViewSmall

            // Swap the FrameLayout tags
            val tag = mainFrame!!.tag as Int
            mainFrame!!.tag = smallFrame.tag
            smallFrame.tag = tag

            // Update the videoFrameMap to keep track of videos
            videoFrameMap!![smallFrame.tag as Int] = smallFrame
            videoFrameMap!![mainFrame!!.tag as Int] = mainFrame
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (agoraManager!!.isJoined) {
            leave()
        }
        onBackPressedDispatcher.onBackPressed()
    }

}