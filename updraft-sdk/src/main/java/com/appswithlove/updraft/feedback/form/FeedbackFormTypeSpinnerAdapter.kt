package com.appswithlove.updraft.feedback.form

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.appswithlove.updraft.R

class FeedbackFormTypeSpinnerAdapter(
    private val context: Context,
    private val feedbackChoices: List<FeedbackChoice>
) : BaseAdapter() {

    override fun getCount(): Int = feedbackChoices.size

    override fun getItem(position: Int): Any = feedbackChoices[position]

    override fun getItemId(position: Int): Long = feedbackChoices[position].id

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val viewHolder: SelectedViewHolder
        val view: View

        if (convertView == null) {
            view = LayoutInflater.from(context)
                .inflate(R.layout.updraft_item_select_spinner, parent, false)
            viewHolder = SelectedViewHolder(view)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as SelectedViewHolder
        }

        val currentItem = getItem(position) as FeedbackChoice
        viewHolder.title.text = currentItem.name

        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val viewHolder: DropdownViewHolder
        val view: View

        if (convertView == null) {
            view = LayoutInflater.from(context)
                .inflate(R.layout.updraft_item_dropdown_spinner, parent, false)
            viewHolder = DropdownViewHolder(view)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as DropdownViewHolder
        }

        val currentItem = getItem(position) as FeedbackChoice
        viewHolder.title.text = currentItem.name

        val layoutParams = viewHolder.wholeView.layoutParams
        layoutParams.height = if (currentItem.isHiddenInDropdown) {
            1
        } else {
            context.resources.getDimensionPixelOffset(R.dimen.updraft_dropdown_item_height)
        }
        viewHolder.wholeView.layoutParams = layoutParams

        return view
    }

    override fun getAutofillOptions(): Array<CharSequence> = emptyArray()

    private class SelectedViewHolder(view: View) {
        val title: TextView = view.findViewById(R.id.updraft_selected_item_title)
    }

    private class DropdownViewHolder(view: View) {
        val wholeView: View = view
        val title: TextView = view.findViewById(R.id.updraft_dropdown_item_title)
    }
}
