package com.app.whispr.ui.groups

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.whispr.data.remote.dto.GroupMessagesDto
import com.app.whispr.data.remote.dto.GroupResponseDto
import com.app.whispr.data.remote.dto.Result
import com.app.whispr.databinding.ItemGroupBinding

class GroupsAdapter(private val onGroupClick: (Result) -> Unit) :
    ListAdapter<Result, GroupsAdapter.GroupHolder>(
        GroupDiffCallback()
    ) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): GroupHolder {
        val binding = ItemGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return GroupHolder(binding)
    }

    override fun onBindViewHolder(
        holder: GroupHolder,
        position: Int
    ) {
        holder.bind(getItem(position))

        holder.itemView.setOnClickListener {

            val currentPosition = holder.bindingAdapterPosition

            if(currentPosition != RecyclerView.NO_POSITION) {
                onGroupClick(
                    getItem(currentPosition)
                )
            }
        }
    }

    class GroupHolder(val binding: ItemGroupBinding) :
            RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Result) {
            binding.tvGroupName.text = item.name
            binding.tvGroupDescription.text = item.description
        }
    }

}