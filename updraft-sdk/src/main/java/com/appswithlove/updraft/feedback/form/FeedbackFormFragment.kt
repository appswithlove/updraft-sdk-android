package com.appswithlove.updraft.feedback.form

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import com.appswithlove.updraft.R
import com.appswithlove.updraft.databinding.FragmentUpdraftFeedbackBinding
import com.appswithlove.updraft.feedback.FeedbackRootContainer

class FeedbackFormFragment : Fragment(), FeedbackFormContract.View {

    private var _binding: FragmentUpdraftFeedbackBinding? = null
    private val binding get() = _binding!!

    private lateinit var presenter: FeedbackFormPresenter
    private var feedbackRootContainer: FeedbackRootContainer? = null

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
        _binding = FragmentUpdraftFeedbackBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            updraftFeedbackPreviousButton.setOnClickListener {
                feedbackRootContainer?.goPrevious()
            }

            val sendButton = updraftFeedbackSendButton
            sendButton.setOnClickListener { presenter.onSendButtonClicked() }
            sendButton.isEnabled = false

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
            updraftFeedbackTypeSpinner.adapter = adapter

            updraftFeedbackTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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

            updraftFeedbackProgressCancel.setOnClickListener {
                presenter.onProgressCancelClicked()
            }
        }

        presenter.attachView(this)
    }

    override fun onDestroyView() {
        presenter.detachView()
        super.onDestroyView()
        _binding = null
    }

    override fun getSelectedChoice(): FeedbackChoice =
        binding.updraftFeedbackTypeSpinner.selectedItem as FeedbackChoice

    override fun getEmail(): String = binding.updraftFeedbackEmail.text.toString()

    override fun getDescription(): String = binding.updraftFeedbackDescription.text.toString()

    override fun showProgress() {
        binding.updraftFeedbackProgressContainer.visibility = View.VISIBLE
    }

    override fun hideProgress() {
        binding.updraftFeedbackProgressContainer.visibility = View.GONE
    }

    override fun showSuccessMessage() {
        with(binding) {
            updraftFeedbackProgressTitle.setText(R.string.updraft_feedback_send_success)
            updraftFeedbackProgressBar.visibility = View.VISIBLE
            updraftFeedbackProgressCancel.visibility = View.GONE
        }
    }

    override fun showErrorMessage(t: Throwable) {
        with(binding) {
            updraftFeedbackProgressTitle.setText(R.string.updraft_feedback_send_failure_title)
            updraftFeedbackProgressBar.visibility = View.GONE
            updraftFeedbackProgressText.setText(R.string.updraft_feedback_send_failure_description)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun updateProgress(progress: Double) {
        with(binding) {
            updraftFeedbackProgressTitle.setText(R.string.updraft_feedback_send_inProgress)
            updraftFeedbackProgressBar.visibility = View.VISIBLE
            updraftFeedbackProgressBar.progress = (progress * 100).toInt()
            updraftFeedbackProgressText.text = "${(progress * 100).toInt()}%"
        }
    }

    override fun closeFeedback() {
        requireActivity().finish()
    }
}
