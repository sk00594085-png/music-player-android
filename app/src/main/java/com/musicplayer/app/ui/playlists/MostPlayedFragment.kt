package com.musicplayer.app.ui.playlists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.musicplayer.app.databinding.FragmentPlaylistDetailBinding
import com.musicplayer.app.ui.songs.SongAdapter
import com.musicplayer.app.viewmodel.MusicViewModel

class MostPlayedFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentPlaylistDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewModel = ViewModelProvider(requireActivity())[MusicViewModel::class.java]

        binding.tvPlaylistTitle.text = "🔥 Most Played"

        val adapter = SongAdapter { index ->
            val list = adapter.currentList
            if (list.isNotEmpty()) {
                viewModel.playSongsAt(list, index)
                dismiss()
            }
        }

        binding.recyclerSongs.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSongs.adapter = adapter

        viewModel.mostPlayedSongs.observe(viewLifecycleOwner) { songs ->
            adapter.submitList(songs)
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
