package com.example.guidesticks_android;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.guidesticks_android.model.ChatMessage;

import java.util.List;

public class ChatMessageAdapter extends BaseAdapter {
    private final LayoutInflater inflater;
    private final List<ChatMessage> messages;

    public ChatMessageAdapter(Context context, List<ChatMessage> messages) {
        this.inflater = LayoutInflater.from(context);
        this.messages = messages;
    }

    @Override
    public int getCount() {
        return messages.size();
    }

    @Override
    public ChatMessage getItem(int position) {
        return messages.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.item_chat_message, parent, false);
        }

        ChatMessage message = getItem(position);
        ChatMessagePresentation presentation = ChatMessagePresentation.from(message.getRole());
        LinearLayout container = view.findViewById(R.id.messageContainer);
        TextView sender = view.findViewById(R.id.tvSender);
        TextView text = view.findViewById(R.id.tvMessage);
        text.setText(message.getContent());

        boolean user = presentation.isUser();
        Context context = inflater.getContext();
        sender.setText(presentation.senderLabelRes());
        sender.setGravity(user ? Gravity.END : Gravity.START);
        sender.setTextColor(ContextCompat.getColor(context,
                user ? R.color.primary_dark : R.color.text_secondary));
        text.setContentDescription(context.getString(
                presentation.accessibilityFormatRes(), message.getContent()));
        text.setGravity(user ? Gravity.END : Gravity.START);
        text.setTextAlignment(user
                ? View.TEXT_ALIGNMENT_VIEW_END
                : View.TEXT_ALIGNMENT_VIEW_START);
        text.setMaxWidth((int) (context.getResources().getDisplayMetrics().widthPixels * 0.88f));

        FrameLayout.LayoutParams layoutParams =
                (FrameLayout.LayoutParams) container.getLayoutParams();
        layoutParams.gravity = user ? Gravity.END : Gravity.START;
        container.setLayoutParams(layoutParams);
        text.setTextColor(ContextCompat.getColor(context,
                user ? R.color.chat_user_text : R.color.text_primary));
        text.setBackgroundResource(user
                ? R.drawable.bg_chat_user
                : R.drawable.bg_chat_assistant);
        int horizontalPadding = dpToPixels(14);
        int verticalPadding = dpToPixels(user ? 11 : 12);
        text.setPadding(horizontalPadding, verticalPadding,
                horizontalPadding, verticalPadding);

        return view;
    }

    public void notifyMessagesChanged() {
        notifyDataSetChanged();
    }

    private int dpToPixels(int dp) {
        return Math.round(dp * inflater.getContext().getResources()
                .getDisplayMetrics().density);
    }
}
