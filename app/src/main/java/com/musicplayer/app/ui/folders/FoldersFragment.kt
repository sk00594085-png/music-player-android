package com.musicplayer.app.ui.folders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.musicplayer.app.MusicPlayerApp
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.musicplayer.app.R
import com.musicplayer.app.databinding.FragmentFoldersBinding
import com.musicplayer.app.viewmodel.MusicViewModel

class FoldersFragment : Fragment() {

    private var _binding: FragmentFoldersBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: MusicViewModel
    private lateinit var adapter: FolderAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFoldersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = (requireActivity().application as MusicPlayerApp).musicViewModel

        adapter = FolderAdapter { folder ->
            val args = Bundle().apply {
                putString("folderPath", folder.path)
                putString("folderName", folder.name)
            }
            findNavController().navigate(R.id.action_folders_to_folder_detail, args)
        }

        binding.recyclerFolders.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerFolders.adapter = adapter

        viewModel.folders.observe(viewLifecycleOwner) { folders ->
            adapter.submitList(folders)
            binding.emptyView.visibility = if (folders.isEmpty()) View.VISIBLE else View.GONE
            binding.folderCount.text = "${folders.size} folders"
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
