package com.musicplayer.app.ui.folders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.musicplayer.app.databinding.FragmentFolderDetailBinding
import com.musicplayer.app.ui.songs.SongAdapter
import com.musicplayer.app.viewmodel.MusicViewModel

class FolderDetailFragment : Fragment() {

    private var _binding: FragmentFolderDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: MusicViewModel
    private lateinit var adapter: SongAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFolderDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[MusicViewModel::class.java]

        val folderPath = arguments?.getString("folderPath") ?: return
        val folderName = arguments?.getString("folderName") ?: "Folder"

        binding.folderDetailTitle.text = folderName

        adapter = SongAdapter { song ->
            val list = adapter.currentList
            val index = list.indexOf(song)
            if (index >= 0) viewModel.playSongsAt(list, index)
        }

        binding.recyclerFolderSongs.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerFolderSongs.adapter = adapter

        viewModel.folders.observe(viewLifecycleOwner) { folders ->
            val folder = folders.find { it.path == folderPath }
            if (folder != null) {
                adapter.submitList(folder.songs)
                binding.folderSongCount.text = "${folder.songCount} songs"
            }
        }

        viewModel.currentSong.observe(viewLifecycleOwner) { song ->
            song?.let { adapter.setActiveSong(it.id) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
