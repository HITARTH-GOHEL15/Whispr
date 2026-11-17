
package com.app.whispr.ui.groups

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
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

            findNavController().navigate(
                R.id.action_groupsFragment_to_chatFragment,
                bundleOf(
                    "groupName" to name,
                )
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
                    Log.d("GROUP_DEBUG", "Loading: ${state.isLoading}")
                    Log.d("GROUP_DEBUG", "Loaded: ${state.groupsLoaded}")
                    Log.d("GROUP_DEBUG", "Success: ${state.success}")
                    Log.d("GROUP_DEBUG", "Groups: ${state.groups.size}")
                    Log.d("GROUP_DEBUG", "Error: ${state.error}")

                    when {

                        state.isLoading -> {
                            binding.tvEmpty.visibility = View.GONE
                        }

                        state.error != null -> {
                            binding.rvGroups.visibility = View.GONE
                            binding.tvEmpty.visibility = View.VISIBLE
                        }

                        state.groupsLoaded -> {

                            val groups = state.groups

                            adapter.submitList(groups)

                            binding.rvGroups.visibility =
                                if (groups.isNotEmpty()) View.VISIBLE
                                else View.GONE

                            binding.tvEmpty.visibility =
                                if (groups.isEmpty()) View.VISIBLE
                                else View.GONE

                            if (groups.isEmpty()) {
                                binding.tvEmpty.text = "No groups found"
                            }
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

        binding.btnCreateGroup.setOnClickListener {
            findNavController().navigate(R.id.action_groupsFragment_to_createGroupFragment)
        }
    }

    override fun onDestroyView() {
        binding.rvGroups.adapter = null
        _binding = null
        super.onDestroyView()
    }
}