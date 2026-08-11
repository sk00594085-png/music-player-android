package com.musicplayer.app.ui.albums

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.musicplayer.app.databinding.FragmentAlbumsBinding
import com.musicplayer.app.viewmodel.MusicViewModel

class AlbumsFragment : Fragment() {

    private var _binding: FragmentAlbumsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: MusicViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlbumsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[MusicViewModel::class.java]

        val adapter = AlbumAdapter { album ->
            AlbumDetailFragment.newInstance(album.name)
                .show(parentFragmentManager, "album_detail")
        }

        binding.recyclerAlbums.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerAlbums.adapter = adapter

        viewModel.songs.observe(viewLifecycleOwner) { songs ->
            val albums = songs.groupBy { it.album.ifBlank { "Unknown Album" } }
                .map { (name, songList) ->
                    AlbumItem(name, songList.first().displayArtist, songList.size, songList.first().albumArtUri)
                }
                .sortedBy { it.name.lowercase() }
            adapter.submitList(albums)
            binding.tvAlbumCount.text = "${albums.size} albums"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
