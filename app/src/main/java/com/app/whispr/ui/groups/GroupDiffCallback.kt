package com.app.whispr.ui.groups

import androidx.recyclerview.widget.DiffUtil
import com.app.whispr.data.remote.dto.Result

class GroupDiffCallback : DiffUtil.ItemCallback<Result>() {
    override fun areItemsTheSame(
        oldItem: Result,
        newItem: Result
    ): Boolean {
        return oldItem.slug == newItem.slug
    }

    override fun areContentsTheSame(
        oldItem: Result,
        newItem: Result
    ): Boolean {
        return oldItem == newItem
    }

}