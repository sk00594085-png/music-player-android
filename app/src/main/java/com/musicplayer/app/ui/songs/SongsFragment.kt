package com.musicplayer.app.ui.songs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.musicplayer.app.MusicPlayerApp
import androidx.recyclerview.widget.LinearLayoutManager
import com.musicplayer.app.R
import com.musicplayer.app.databinding.FragmentSongsBinding
import com.musicplayer.app.model.Song
import com.musicplayer.app.ui.main.MainActivity
import com.musicplayer.app.ui.playlists.AddToPlaylistDialog
import com.musicplayer.app.utils.AppPreferences
import com.musicplayer.app.viewmodel.MusicViewModel

class SongsFragment : Fragment() {

    private var _binding: FragmentSongsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: MusicViewModel
    private lateinit var adapter: SongAdapter
    private var allSongs: List<Song> = emptyList()
    private var currentQuery: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSongsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = (requireActivity().application as MusicPlayerApp).musicViewModel

        adapter = SongAdapter(
            onSongClick = { song ->
                val list = adapter.currentList
                val index = list.indexOf(song)
                if (index >= 0) viewModel.playSongsAt(list, index)
            },
            onFavouriteClick = { song ->
                viewModel.toggleFavourite(song)
            },
            onSongLongClick = { song ->
                showSongContextMenu(song)
            }
        )

        binding.recyclerSongs.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSongs.adapter = adapter

        viewModel.songs.observe(viewLifecycleOwner) { songs ->
            allSongs = songs
            applySortAndFilter()
            binding.songCount.text = "${songs.size} songs"
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.currentSong.observe(viewLifecycleOwner) { song ->
            song?.let { adapter.setActiveSong(it.id) }
        }

        viewModel.favouriteSongs.observe(viewLifecycleOwner) { favSongs ->
            adapter.setFavouriteIds(favSongs.map { it.id }.toSet())
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(q: String?) = false
            override fun onQueryTextChange(q: String?): Boolean {
                currentQuery = q.orEmpty()
                applySortAndFilter()
                return true
            }
        })

        binding.btnSort.setOnClickListener { showSortMenu(it) }
    }

    private fun applySortAndFilter() {
        val query = currentQuery
        val filtered = if (query.isBlank()) allSongs
        else allSongs.filter {
            it.displayTitle.contains(query, ignoreCase = true) ||
                    it.displayArtist.contains(query, ignoreCase = true)
        }

        val sortBy = AppPreferences.getSortBy(requireContext())
        val asc = AppPreferences.isSortAscending(requireContext())

        val sorted = when (sortBy) {
            "artist"   -> if (asc) filtered.sortedBy { it.displayArtist.lowercase() }
                          else filtered.sortedByDescending { it.displayArtist.lowercase() }
            "duration" -> if (asc) filtered.sortedBy { it.duration }
                          else filtered.sortedByDescending { it.duration }
            "date"     -> if (asc) filtered.sortedBy { it.dateAdded }
                          else filtered.sortedByDescending { it.dateAdded }
            "size"     -> if (asc) filtered.sortedBy { it.size }
                          else filtered.sortedByDescending { it.size }
            else       -> if (asc) filtered.sortedBy { it.displayTitle.lowercase() }
                          else filtered.sortedByDescending { it.displayTitle.lowercase() }
        }

        adapter.submitList(sorted)
        binding.emptyView.visibility = if (sorted.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showSortMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.menu_sort, popup.menu)

        val currentSort = AppPreferences.getSortBy(requireContext())
        val isAsc = AppPreferences.isSortAscending(requireContext())

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.sort_title   -> setSortOrder("title", isAsc)
                R.id.sort_artist  -> setSortOrder("artist", isAsc)
                R.id.sort_duration -> setSortOrder("duration", isAsc)
                R.id.sort_date    -> setSortOrder("date", isAsc)
                R.id.sort_size    -> setSortOrder("size", isAsc)
                R.id.sort_toggle_order -> setSortOrder(currentSort, !isAsc)
            }
            true
        }
        popup.show()
    }

    private fun setSortOrder(by: String, asc: Boolean) {
        AppPreferences.setSortBy(requireContext(), by)
        AppPreferences.setSortAscending(requireContext(), asc)
        applySortAndFilter()
    }

    private fun showSongContextMenu(song: Song) {
        val popup = PopupMenu(requireContext(), binding.recyclerSongs)
        popup.menuInflater.inflate(R.menu.menu_song_context, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_add_to_playlist -> {
                    AddToPlaylistDialog.newInstance(song).show(parentFragmentManager, "add_to_playlist")
                    true
                }
                R.id.action_toggle_favourite -> {
                    viewModel.toggleFavourite(song)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
