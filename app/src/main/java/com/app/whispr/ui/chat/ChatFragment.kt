package com.app.whispr.ui.chat

import android.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.app.whispr.databinding.FragmentChatBinding
import com.app.whispr.ui.viewModel.WebSocketViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChatFragment : androidx.fragment.app.Fragment() {

    private var _binding: FragmentChatBinding? = null

    private val binding get() = _binding!!

    private val viewModel: WebSocketViewModel by viewModels()

    private lateinit var adapter: ChatMessageSendAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun setupRecycleView() {

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}