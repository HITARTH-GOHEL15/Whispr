package com.app.whispr.ui.chat

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.app.whispr.data.remote.dto.Message
import com.app.whispr.databinding.FragmentChatBinding
import com.app.whispr.databinding.ItemMessageReceivedBinding

class ChatReceivedAdapter(

) {
    class ChatReceivedHolder(
        private val binding: ItemMessageReceivedBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Message) {
            binding.tvMessage.text = item.content
            binding.tvTime.text = item.createdAt
            binding.tvSender.text = item.sender.toString()
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