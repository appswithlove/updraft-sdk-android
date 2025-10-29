package com.appswithlove.updraft.feedback.drawing

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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
import androidx.core.view.doOnLayout
import com.appswithlove.updraft.databinding.FragmentUpdraftDrawingBinding

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

    private var _binding: FragmentUpdraftDrawingBinding? = null
    private val binding get() = _binding!!

    private var feedbackRootContainer: FeedbackRootContainer? = null
    private var currentBitmap: Bitmap? = null

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
        _binding = FragmentUpdraftDrawingBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(binding) {
            updraftDrawingView.setPaintWidthPx(6f)

            val colorSelectRadioGroup: RadioGroup = binding.updraftColorSelectRadiogroup
            colorSelectRadioGroup.setOnCheckedChangeListener { group, checkedId ->
                when (checkedId) {
                    updraftColorButtonBlack.id -> updraftDrawingView.setPaintColor(Color.BLACK)
                    updraftColorButtonWhite.id -> updraftDrawingView.setPaintColor(Color.WHITE)
                    updraftColorButtonYellow.id -> updraftDrawingView.setPaintColor(
                        ResourcesCompat.getColor(resources, R.color.updraft_yellow, null)
                    )

                    updraftColorButtonRed.id -> updraftDrawingView.setPaintColor(
                        ResourcesCompat.getColor(resources, R.color.updraft_red, null)
                    )
                }
            }

            updraftColorSelectResetButton.setOnClickListener {
                updraftDrawingView.undoLast()
            }

            try {
                val inputStream = requireContext().openFileInput(fileName ?: return)
                currentBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            updateBitmap()

            updraftFeedbackNextButton.setOnClickListener {
                updraftDrawingView.getDrawScreenshot(object : FreeDrawView.DrawCreatorListener {
                    override fun onDrawCreated(draw: Bitmap) {
                        saveBitmap(draw)
                    }

                    override fun onDrawCreationError() {
                        saveBitmap(null)
                    }
                })
            }

            val drawSomethingHereContainer = updraftDrawHereContainer
            drawSomethingHereContainer.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    v.performClick()
                    v.visibility = View.GONE
                }
                false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateBitmap() {
        binding.updraftScreenshotBitmapHolder.doOnLayout { view ->
            val imageView = view as ImageView
            imageView.setImageBitmap(currentBitmap)

            val drawable = imageView.drawable ?: return@doOnLayout

            // Original image size
            val intrinsicWidth = drawable.intrinsicWidth
            val intrinsicHeight = drawable.intrinsicHeight

            // Actual ImageView size
            val imageViewWidth = imageView.width.toFloat()
            val imageViewHeight = imageView.height.toFloat()

            // ImageView scaleType
            val scale: Float
            val dx: Float
            val dy: Float
            if (intrinsicWidth * imageViewHeight > imageViewWidth * intrinsicHeight) {
                // Image is taller than the view
                scale = imageViewWidth / intrinsicWidth
                dx = 0f
                dy = (imageViewHeight - intrinsicHeight * scale) / 2f
            } else {
                // Image is wider than the view
                scale = imageViewHeight / intrinsicHeight
                dx = (imageViewWidth - intrinsicWidth * scale) / 2f
                dy = 0f
            }

            val displayedImageWidth = intrinsicWidth * scale
            val displayedImageHeight = intrinsicHeight * scale

            // Set FreeDrawView to match displayed area
            val params = binding.updraftDrawingView.layoutParams as FrameLayout.LayoutParams
            params.width = displayedImageWidth.toInt()
            params.height = displayedImageHeight.toInt()
            params.leftMargin = dx.toInt()
            params.topMargin = dy.toInt()
            binding.updraftDrawingView.layoutParams = params
        }
    }

    private fun saveBitmap(drawnBitmap: Bitmap?) {
        val baseBitmap = currentBitmap ?: return
        val bitmapConfig = baseBitmap.config ?: Bitmap.Config.ARGB_8888

        // Create a bitmap with the same size as the original
        val bmOverlay = createBitmap(baseBitmap.width, baseBitmap.height, bitmapConfig)
        val canvas = Canvas(bmOverlay)

        // Draw the base bitmap
        canvas.drawBitmap(baseBitmap, 0f, 0f, null)

        // Draw the overlay bitmap scaled to match the base bitmap
        drawnBitmap?.let { overlay ->
            if (overlay.width != baseBitmap.width || overlay.height != baseBitmap.height) {
                val matrix = Matrix().apply {
                    val scaleX = baseBitmap.width.toFloat() / overlay.width
                    val scaleY = baseBitmap.height.toFloat() / overlay.height
                    postScale(scaleX, scaleY)
                }
                canvas.drawBitmap(overlay, matrix, null)
            } else {
                canvas.drawBitmap(overlay, 0f, 0f, null)
            }
        }

        try {
            requireContext().openFileOutput(FeedbackActivity.SAVED_SCREENSHOT, Context.MODE_PRIVATE)
                .use { stream ->
                    bmOverlay.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
            bmOverlay.recycle()
            feedbackRootContainer?.goNext()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                requireContext(),
                R.string.updraft_global_error,
                Toast.LENGTH_SHORT,
            ).show()
        }
    }
}
