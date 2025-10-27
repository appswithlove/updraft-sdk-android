package com.appswithlove.updraft.feedback.drawing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.appswithlove.updraft.R
import com.appswithlove.updraft.feedback.FeedbackActivity
import com.appswithlove.updraft.feedback.FeedbackRootContainer
import com.rm.freedrawview.FreeDrawView
import androidx.core.graphics.createBitmap

class UpdraftDrawingFragment : Fragment() {

    companion object {
        private const val FILENAME_ARG = "filename"

        fun getInstance(fileName: String): UpdraftDrawingFragment {
            return UpdraftDrawingFragment().apply {
                arguments = Bundle().apply {
                    putString(FILENAME_ARG, fileName)
                }
            }
        }
    }

    private var feedbackRootContainer: FeedbackRootContainer? = null
    private var currentBitmap: Bitmap? = null
    private lateinit var screenShotBitmapHolder: ImageView
    private lateinit var freeDrawView: FreeDrawView
    private var fileName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fileName = arguments?.getString(FILENAME_ARG)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FeedbackRootContainer) {
            feedbackRootContainer = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        feedbackRootContainer = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_updraft_drawing, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        screenShotBitmapHolder = view.findViewById(R.id.updraft_screenshot_bitmap_holder)
        freeDrawView = view.findViewById(R.id.updraft_drawing_view)
        freeDrawView.setPaintWidthPx(6f)

        val colorSelectRadioGroup: RadioGroup =
            view.findViewById(R.id.updraft_color_select_radiogroup)
        colorSelectRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.updraft_color_button_black -> freeDrawView.setPaintColor(Color.BLACK)
                R.id.updraft_color_button_white -> freeDrawView.setPaintColor(Color.WHITE)
                R.id.updraft_color_button_yellow -> freeDrawView.setPaintColor(
                    ResourcesCompat.getColor(resources, R.color.updraft_yellow, null)
                )

                R.id.updraft_color_button_red -> freeDrawView.setPaintColor(
                    ResourcesCompat.getColor(resources, R.color.updraft_red, null)
                )
            }
        }

        view.findViewById<View>(R.id.updraft_color_select_reset_button).setOnClickListener {
            freeDrawView.undoLast()
        }

        try {
            val inputStream = requireContext().openFileInput(fileName ?: return)
            currentBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        updateBitmap()

        view.findViewById<View>(R.id.updraft_feedback_next_button).setOnClickListener {
            freeDrawView.getDrawScreenshot(object : FreeDrawView.DrawCreatorListener {
                override fun onDrawCreated(draw: Bitmap) {
                    saveBitmap(draw)
                }

                override fun onDrawCreationError() {
                    saveBitmap(null)
                }
            })
        }

        val drawSomethingHereContainer: View = view.findViewById(R.id.updraft_draw_here_container)
        drawSomethingHereContainer.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                v.performClick()
                v.visibility = View.GONE
            }
            false
        }
    }

    private fun updateBitmap() {
        Handler(Looper.getMainLooper()).post {
            screenShotBitmapHolder.setImageBitmap(currentBitmap)
        }
    }

    private fun saveBitmap(drawnBitmap: Bitmap?) {
        val baseBitmap = currentBitmap ?: return
        val bitmapConfig = baseBitmap.config ?: return
        val bmOverlay = createBitmap(
            baseBitmap.width,
            baseBitmap.height,
            bitmapConfig
        )

        val canvas = Canvas(bmOverlay)
        canvas.drawBitmap(baseBitmap, Matrix(), null)
        drawnBitmap?.let { canvas.drawBitmap(it, Matrix(), null) }

        try {
            requireContext().openFileOutput(FeedbackActivity.SAVED_SCREENSHOT, Context.MODE_PRIVATE)
                .use { stream ->
                    bmOverlay.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
            bmOverlay.recycle()
            feedbackRootContainer?.goNext()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), R.string.updraft_global_error, Toast.LENGTH_SHORT)
                .show()
        }
    }
}
