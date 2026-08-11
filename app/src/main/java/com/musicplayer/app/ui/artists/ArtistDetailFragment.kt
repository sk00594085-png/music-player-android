package com.musicplayer.app.ui.artists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.musicplayer.app.databinding.FragmentArtistDetailBinding
import com.musicplayer.app.ui.songs.SongAdapter
import com.musicplayer.app.viewmodel.MusicViewModel

class ArtistDetailFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentArtistDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: SongAdapter

    companion object {
        private const val ARG_ARTIST = "artist_name"
        fun newInstance(artistName: String) = ArtistDetailFragment().apply {
            arguments = Bundle().apply { putString(ARG_ARTIST, artistName) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArtistDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val artistName = arguments?.getString(ARG_ARTIST) ?: return
        val viewModel = ViewModelProvider(requireActivity())[MusicViewModel::class.java]

        binding.tvArtistTitle.text = artistName

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
            adapter.submitList(songs.filter { it.displayArtist == artistName })
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
