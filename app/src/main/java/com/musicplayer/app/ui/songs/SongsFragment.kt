package com.musicplayer.app.ui.songs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.musicplayer.app.databinding.FragmentSongsBinding
import com.musicplayer.app.model.Song
import com.musicplayer.app.ui.main.MainActivity
import com.musicplayer.app.viewmodel.MusicViewModel

class SongsFragment : Fragment() {

    private var _binding: FragmentSongsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: MusicViewModel
    private lateinit var adapter: SongAdapter
    private var allSongs: List<Song> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSongsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[MusicViewModel::class.java]

        adapter = SongAdapter { index ->
            val filteredSongs = adapter.currentList
            if (filteredSongs.isEmpty()) return@SongAdapter
            viewModel.playSongsAt(filteredSongs, index)
        }

        binding.recyclerSongs.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSongs.adapter = adapter

        viewModel.songs.observe(viewLifecycleOwner) { songs ->
            allSongs = songs
            adapter.submitList(songs)
            binding.emptyView.visibility = if (songs.isEmpty()) View.VISIBLE else View.GONE
            binding.songCount.text = "${songs.size} songs"
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.currentSong.observe(viewLifecycleOwner) { song ->
            song?.let { adapter.setActiveSong(it.id) }
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(q: String?) = false
            override fun onQueryTextChange(q: String?): Boolean {
                filterSongs(q.orEmpty())
                return true
            }
        })
    }

    private fun filterSongs(query: String) {
        val filtered = if (query.isBlank()) allSongs
        else allSongs.filter {
            it.displayTitle.contains(query, ignoreCase = true) ||
                    it.displayArtist.contains(query, ignoreCase = true)
        }
        adapter.submitList(filtered)
        binding.emptyView.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
