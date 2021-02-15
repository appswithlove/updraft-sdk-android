package com.appswithlove.updraft.feedback.form;

import android.content.Context;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.appswithlove.updraft.R;

import java.util.List;

public class FeedbackFormTypeSpinnerAdapter extends BaseAdapter {

    private List<FeedbackChoice> mFeedbackChoices;
    private Context mContext;

    public FeedbackFormTypeSpinnerAdapter(Context context, List<FeedbackChoice> feedbackChoices) {
        mContext = context;
        mFeedbackChoices = feedbackChoices;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        DropdownViewHolder viewHolder;

        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.updraft_item_dropdown_spinner, parent, false);
            viewHolder = new DropdownViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (DropdownViewHolder) convertView.getTag();
        }

        FeedbackChoice currentItem = (FeedbackChoice) getItem(position);
        viewHolder.title.setText(currentItem.getName());
        if (currentItem.isHiddenInDropdown()) {
            ViewGroup.LayoutParams l = viewHolder.wholeView.getLayoutParams();
            l.height = 1;
            viewHolder.wholeView.setLayoutParams(l);
        } else {
            ViewGroup.LayoutParams l = viewHolder.wholeView.getLayoutParams();
            l.height = mContext.getResources().getDimensionPixelOffset(R.dimen.updraft_dropdown_item_height);
            viewHolder.wholeView.setLayoutParams(l);
        }
        return convertView;

    }

    @Override
    public int getCount() {
        return mFeedbackChoices.size();
    }

    @Override
    public Object getItem(int i) {
        return mFeedbackChoices.get(i);
    }

    @Override
    public long getItemId(int i) {
        return mFeedbackChoices.get(i).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SelectedViewHolder viewHolder;

        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.updraft_item_select_spinner, parent, false);
            viewHolder = new SelectedViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (SelectedViewHolder) convertView.getTag();
        }

        FeedbackChoice currentItem = (FeedbackChoice) getItem(position);
        viewHolder.title.setText(currentItem.getName());
        return convertView;
    }

    @Nullable
    @Override
    public CharSequence[] getAutofillOptions() {
        return new CharSequence[0];
    }

    private static class SelectedViewHolder {

        TextView title;

        SelectedViewHolder(View view) {
            title = view.findViewById(R.id.updraft_selected_item_title);
        }
    }

    private static class DropdownViewHolder {

        View wholeView;
        TextView title;

        DropdownViewHolder(View view) {
            wholeView = view;
            title = view.findViewById(R.id.updraft_dropdown_item_title);
        }

    }
}
