package com.musicplayer.app.ui.albums

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.musicplayer.app.databinding.FragmentAlbumDetailBinding
import com.musicplayer.app.ui.songs.SongAdapter
import com.musicplayer.app.viewmodel.MusicViewModel

class AlbumDetailFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentAlbumDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: SongAdapter

    companion object {
        private const val ARG_ALBUM = "album_name"
        fun newInstance(albumName: String) = AlbumDetailFragment().apply {
            arguments = Bundle().apply { putString(ARG_ALBUM, albumName) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlbumDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val albumName = arguments?.getString(ARG_ALBUM) ?: return
        val viewModel = ViewModelProvider(requireActivity())[MusicViewModel::class.java]

        binding.tvAlbumTitle.text = albumName

        adapter = SongAdapter { index ->
            val list = adapter.currentList
            if (list.isNotEmpty()) {
                viewModel.playSongsAt(list, index)
                dismiss()
            }
        }

        binding.recyclerSongs.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSongs.adapter = adapter

        viewModel.songs.observe(viewLifecycleOwner) { songs ->
            adapter.submitList(songs.filter {
                it.album.ifBlank { "Unknown Album" } == albumName
            })
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
