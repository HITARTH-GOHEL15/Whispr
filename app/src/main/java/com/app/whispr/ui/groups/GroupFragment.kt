
package com.app.whispr.ui.groups

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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.whispr.R
import com.app.whispr.databinding.FragmentGroupsBinding
import com.app.whispr.ui.viewModel.RestApiViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GroupsFragment : Fragment() {

    private var _binding: FragmentGroupsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RestApiViewModel by viewModels()

    private lateinit var adapter: GroupsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupsBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root
    }

    private fun setupRecyclerView() {

        adapter = GroupsAdapter { result ->

            val name = result.name ?: return@GroupsAdapter

            // Navigate to ChatFragment.
            // Pass the room slug/name as arguments when your
            // navigation graph and ChatFragment arguments are ready.

            findNavController().navigate(
                R.id.action_groupsFragment_to_chatFragment
            )
        }

        binding.rvGroups.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@GroupsFragment.adapter
            setHasFixedSize(false)
        }
    }

    private fun observeUiState() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                viewModel.uiState.collect { state ->

                    binding.progressBar.visibility =
                        if (state.isLoading) View.VISIBLE else View.GONE

                    when {

                        state.isLoading -> {
                            binding.tvEmpty.visibility = View.GONE
                        }

                        state.success -> {

                            val groups = state.groups

                            // Adapt this line if your API returns
                            // a nullable list.
                            adapter.submitList(groups)

                            binding.tvEmpty.visibility =
                                if (groups.isEmpty()) View.VISIBLE else View.GONE

                            binding.rvGroups.visibility =
                                if (groups.isEmpty()) View.GONE else View.VISIBLE
                        }

                        state.error != null -> {

                            binding.rvGroups.visibility = View.GONE
                            binding.tvEmpty.visibility = View.VISIBLE

                            binding.tvEmpty.text =
                                state.error.errorMessage ?: "Something went wrong"

                            Toast.makeText(
                                requireContext(),
                                state.error.errorMessage ?: "Failed to load groups",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeUiState()

        viewModel.getGroups()
    }

    override fun onDestroyView() {
        binding.rvGroups.adapter = null
        _binding = null
        super.onDestroyView()
    }
}