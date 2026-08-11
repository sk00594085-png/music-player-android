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

class PlaylistDetailFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentPlaylistDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: SongAdapter

    companion object {
        private const val ARG_ID   = "playlist_id"
        private const val ARG_NAME = "playlist_name"
        fun newInstance(id: Long, name: String) = PlaylistDetailFragment().apply {
            arguments = Bundle().apply {
                putLong(ARG_ID, id)
                putString(ARG_NAME, name)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val playlistId   = arguments?.getLong(ARG_ID) ?: return
        val playlistName = arguments?.getString(ARG_NAME) ?: ""
        val viewModel    = ViewModelProvider(requireActivity())[MusicViewModel::class.java]

        binding.tvPlaylistTitle.text = playlistName

        adapter = SongAdapter { song ->
            val list = adapter.currentList
            val index = list.indexOf(song)
            if (index >= 0) {
                viewModel.playSongsAt(list, index)
                dismiss()
            }
        }

        binding.recyclerSongs.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSongs.adapter = adapter

        viewModel.getPlaylistSongs(playlistId).observe(viewLifecycleOwner) { songs ->
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
