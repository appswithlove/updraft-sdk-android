package com.appswithlove.updraft.feedback

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.appswithlove.updraft.R
import com.appswithlove.updraft.Updraft
import com.appswithlove.updraft.databinding.FeedbackActivityBinding
import com.appswithlove.updraft.feedback.drawing.UpdraftDrawingFragment
import com.appswithlove.updraft.feedback.form.FeedbackFormFragment

class FeedbackActivity : AppCompatActivity(), FeedbackRootContainer {

    private var _binding: FeedbackActivityBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val SAVED_SCREENSHOT = "UPDRAFT_SAVED_SCREENSHOT.png"
        private const val FILENAME_ARG = "filename"

        fun getIntent(context: Context, filename: String): Intent {
            return Intent(context, FeedbackActivity::class.java).apply {
                putExtra(FILENAME_ARG, filename)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        _binding = FeedbackActivityBinding.inflate(layoutInflater)
        val rootView = binding.root
        setContentView(rootView)

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                systemBarsInsets.left,
                systemBarsInsets.top,
                systemBarsInsets.right,
                systemBarsInsets.bottom
            )
            insets
        }

        if (savedInstanceState == null) {
            val filename = intent.getStringExtra(FILENAME_ARG)
            supportFragmentManager.beginTransaction()
                .replace(
                    binding.updraftFeedbackRootContainer.id,
                    UpdraftDrawingFragment.getInstance(filename ?: "")
                )
                .commit()
        }

        binding.updraftCloseButton.setOnClickListener {
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        Updraft.getInstance()?.shakeDetectorManager?.stopListening()
    }

    override fun onStop() {
        Updraft.getInstance()?.shakeDetectorManager?.startListening()
        super.onStop()
    }

    override fun goNext() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.updraft_feedback_root_container, FeedbackFormFragment())
            .addToBackStack(null)
            .commit()
    }

    override fun goPrevious() {
        supportFragmentManager.popBackStack()
    }
}
