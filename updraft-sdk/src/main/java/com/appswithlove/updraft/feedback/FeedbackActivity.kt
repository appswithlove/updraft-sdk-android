package com.appswithlove.updraft.feedback

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.appswithlove.updraft.R
import com.appswithlove.updraft.Updraft
import com.appswithlove.updraft.feedback.drawing.UpdraftDrawingFragment
import com.appswithlove.updraft.feedback.form.FeedbackFormFragment

class FeedbackActivity : AppCompatActivity(), FeedbackRootContainer {

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
        setContentView(R.layout.feedback_activity)

        if (savedInstanceState == null) {
            val filename = intent.getStringExtra(FILENAME_ARG)
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.updraft_feedback_root_container,
                    UpdraftDrawingFragment.getInstance(filename ?: "")
                )
                .commit()
        }

        findViewById<View>(R.id.updraft_close_button).setOnClickListener {
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
