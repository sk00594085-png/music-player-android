package com.musicplayer.app.ui.playlists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.musicplayer.app.MusicPlayerApp
import androidx.recyclerview.widget.LinearLayoutManager
import com.musicplayer.app.R
import com.musicplayer.app.databinding.FragmentPlaylistsBinding
import com.musicplayer.app.viewmodel.MusicViewModel

class PlaylistsFragment : Fragment() {

    private var _binding: FragmentPlaylistsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: MusicViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = (requireActivity().application as MusicPlayerApp).musicViewModel

        val adapter = PlaylistSectionAdapter(
            onPlaylistClick = { playlist ->
                PlaylistDetailFragment.newInstance(playlist.id, playlist.name)
                    .show(parentFragmentManager, "playlist_detail")
            },
            onPlaylistLongClick = { playlist ->
                showPlaylistOptions(playlist)
            },
            onFavouritesClick = {
                FavouritesFragment().show(parentFragmentManager, "favourites")
            },
            onRecentlyPlayedClick = {
                RecentlyPlayedFragment().show(parentFragmentManager, "recently_played")
            },
            onMostPlayedClick = {
                MostPlayedFragment().show(parentFragmentManager, "most_played")
            }
        )

        binding.recyclerPlaylists.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerPlaylists.adapter = adapter

        viewModel.playlists.observe(viewLifecycleOwner) { playlists ->
            adapter.submitPlaylists(playlists)
        }

        binding.fabNewPlaylist.setOnClickListener {
            showCreatePlaylistDialog()
        }
    }

    private fun showCreatePlaylistDialog() {
        val et = EditText(requireContext()).apply { hint = getString(R.string.playlist_name_hint) }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.create_playlist)
            .setView(et)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = et.text.toString().trim()
                if (name.isNotEmpty()) viewModel.createPlaylist(name)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showPlaylistOptions(playlist: com.musicplayer.app.db.PlaylistEntity) {
        val options = arrayOf(
            getString(R.string.rename_playlist),
            getString(R.string.delete_playlist)
        )
        AlertDialog.Builder(requireContext())
            .setTitle(playlist.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showRenameDialog(playlist)
                    1 -> viewModel.deletePlaylist(playlist)
                }
            }
            .show()
    }

    private fun showRenameDialog(playlist: com.musicplayer.app.db.PlaylistEntity) {
        val et = EditText(requireContext()).apply { setText(playlist.name) }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.rename_playlist)
            .setView(et)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = et.text.toString().trim()
                if (name.isNotEmpty()) viewModel.renamePlaylist(playlist, name)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
