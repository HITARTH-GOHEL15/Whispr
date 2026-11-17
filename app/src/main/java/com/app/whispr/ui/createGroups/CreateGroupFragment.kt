package com.app.whispr.ui.createGroups

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.app.whispr.databinding.FragmentCreateGroupBinding
import com.app.whispr.ui.viewModel.RestApiViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CreateGroupFragment : Fragment() {

    private var _binding: FragmentCreateGroupBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RestApiViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateGroupBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupCreateButton()
        observeCreateGroupState()
    }

    private fun setupCreateButton() {

        binding.btnCreate.setOnClickListener {

            val name = binding.etGroupName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()
            val slug = binding.etGroupSlug.text.toString().trim()

            // Basic validation
            if (name.isBlank()) {
                binding.etGroupName.error = "Enter group name"
                return@setOnClickListener
            }

            if (email.isBlank()) {
                binding.etEmail.error = "Enter email"
                return@setOnClickListener
            }

            if (slug.isBlank()) {
                binding.etGroupSlug.error = "Enter group slug"
                return@setOnClickListener
            }

            viewModel.createGroup(
                name = name,
                email = email,
                description = description,
                slug = slug
            )
        }
    }

    private fun observeCreateGroupState() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                viewModel.uiState.collect { state ->

                    when {
                        state.isLoading -> {
                            binding.btnCreate.isEnabled = false
                        }

                        state.error != null -> {
                            binding.btnCreate.isEnabled = true

                            Toast.makeText(
                                requireContext(),
                                state.error.errorMessage
                                    ?: "Group creation failed",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        state.success -> {
                            binding.btnCreate.isEnabled = true

                            Toast.makeText(
                                requireContext(),
                                "Group created successfully",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}