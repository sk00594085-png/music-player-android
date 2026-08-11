package com.musicplayer.app.ui.artists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.musicplayer.app.databinding.FragmentArtistsBinding
import com.musicplayer.app.viewmodel.MusicViewModel

class ArtistsFragment : Fragment() {

    private var _binding: FragmentArtistsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArtistsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewModel = ViewModelProvider(requireActivity())[MusicViewModel::class.java]

        val adapter = ArtistAdapter { artist ->
            ArtistDetailFragment.newInstance(artist.name)
                .show(parentFragmentManager, "artist_detail")
        }

        binding.recyclerArtists.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerArtists.adapter = adapter

        viewModel.songs.observe(viewLifecycleOwner) { songs ->
            val artists = songs.groupBy { it.displayArtist }
                .map { (name, songList) -> ArtistItem(name, songList.size) }
                .sortedBy { it.name.lowercase() }
            adapter.submitList(artists)
            binding.tvArtistCount.text = "${artists.size} artists"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
