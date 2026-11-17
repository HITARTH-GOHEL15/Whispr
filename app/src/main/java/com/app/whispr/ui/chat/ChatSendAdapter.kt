package com.app.whispr.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.whispr.data.remote.dto.Message
import com.app.whispr.databinding.ItemMessageSendBinding

class ChatMessageSendAdapter : ListAdapter<Message, ChatMessageSendAdapter.ChatHolder>(
    DIFF_CALLBACK
) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ChatHolder {
        val binding = ItemMessageSendBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ChatHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ChatHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }


    class ChatHolder(
        private val binding: ItemMessageSendBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Message) {

            binding.tvMessage.text = item.content
            binding.tvTime.text = item.createdAt
        }
    }

    companion object {

        private val DIFF_CALLBACK =
            object : DiffUtil.ItemCallback<Message>() {
                override fun areItemsTheSame(
                    oldItem: Message,
                    newItem: Message
                ): Boolean {
                    return oldItem == newItem
                }

                override fun areContentsTheSame(
                    oldItem: Message,
                    newItem: Message
                ): Boolean {
                    return oldItem == newItem
                }

            }
    }
}