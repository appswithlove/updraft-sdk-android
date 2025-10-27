package com.appswithlove.updraft.feedback.form

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.appswithlove.updraft.R
import com.appswithlove.updraft.feedback.FeedbackRootContainer

class FeedbackFormFragment : Fragment(), FeedbackFormContract.View {

    private lateinit var presenter: FeedbackFormPresenter
    private var feedbackRootContainer: FeedbackRootContainer? = null
    private lateinit var spinner: Spinner
    private lateinit var emailEdit: EditText
    private lateinit var descriptionEdit: EditText
    private lateinit var progress: View
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var progressTitle: TextView
    private lateinit var progressCancelButton: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter = FeedbackFormPresenter()
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
        return inflater.inflate(R.layout.fragment_updraft_feedback, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progress = view.findViewById(R.id.updraft_feedback_progress_container)

        view.findViewById<View>(R.id.updraft_feedback_previous_button).setOnClickListener {
            feedbackRootContainer?.goPrevious()
        }

        emailEdit = view.findViewById(R.id.updraft_feedback_email)
        descriptionEdit = view.findViewById(R.id.updraft_feedback_description)

        val sendButton: Button = view.findViewById(R.id.updraft_feedback_send_button)
        sendButton.setOnClickListener { presenter.onSendButtonClicked() }
        sendButton.isEnabled = false

        spinner = view.findViewById(R.id.updraft_feedback_type_spinner)

        val feedbackChoices = listOf(
            FeedbackChoice(
                FeedbackChoice.FEEDBACK_TYPE_NOT_SELECTED,
                getString(R.string.updraft_feedback_type_title),
                true
            ),
            FeedbackChoice(
                FeedbackChoice.FEEDBACK_TYPE_DESIGN,
                getString(R.string.updraft_feedback_type_design),
                false
            ),
            FeedbackChoice(
                FeedbackChoice.FEEDBACK_TYPE_FEEDBACK,
                getString(R.string.updraft_feedback_type_feedback),
                false
            ),
            FeedbackChoice(
                FeedbackChoice.FEEDBACK_TYPE_BUG,
                getString(R.string.updraft_feedback_type_bug),
                false
            )
        )

        val adapter = FeedbackFormTypeSpinnerAdapter(requireContext(), feedbackChoices)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val feedbackChoice = parent.selectedItem as FeedbackChoice
                sendButton.isEnabled = !feedbackChoice.isHiddenInDropdown
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                sendButton.isEnabled = false
            }
        }

        progressBar = view.findViewById(R.id.updraft_feedback_progress_bar)
        progressText = view.findViewById(R.id.updraft_feedback_progress_text)
        progressCancelButton = view.findViewById(R.id.updraft_feedback_progress_cancel)
        progressCancelButton.setOnClickListener {
            presenter.onProgressCancelClicked()
        }

        progressTitle = view.findViewById(R.id.updraft_feedback_progress_title)

        presenter.attachView(this)
    }

    override fun onDestroyView() {
        presenter.detachView()
        super.onDestroyView()
    }

    override fun getSelectedChoice(): FeedbackChoice =
        spinner.selectedItem as FeedbackChoice

    override fun getEmail(): String = emailEdit.text.toString()

    override fun getDescription(): String = descriptionEdit.text.toString()

    override fun showProgress() {
        progress.visibility = View.VISIBLE
    }

    override fun hideProgress() {
        progress.visibility = View.GONE
    }

    override fun showSuccessMessage() {
        progressTitle.setText(R.string.updraft_feedback_send_success)
        progressBar.visibility = View.VISIBLE
        progressCancelButton.visibility = View.GONE
    }

    override fun showErrorMessage(t: Throwable) {
        progressTitle.setText(R.string.updraft_feedback_send_failure_title)
        progressBar.visibility = View.GONE
        progressText.setText(R.string.updraft_feedback_send_failure_description)
    }

    @SuppressLint("SetTextI18n")
    override fun updateProgress(progress: Double) {
        progressTitle.setText(R.string.updraft_feedback_send_inProgress)
        progressBar.visibility = View.VISIBLE
        progressBar.progress = (progress * 100).toInt()
        progressText.text = "${(progress * 100).toInt()}%"
    }

    override fun closeFeedback() {
        requireActivity().finish()
    }
}
