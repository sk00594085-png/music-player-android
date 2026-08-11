package com.musicplayer.app.ui.playlists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.musicplayer.app.databinding.DialogAddToPlaylistBinding
import com.musicplayer.app.model.Song
import com.musicplayer.app.viewmodel.MusicViewModel

class AddToPlaylistDialog : BottomSheetDialogFragment() {

    private var _binding: DialogAddToPlaylistBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_SONG_ID    = "song_id"
        private const val ARG_SONG_PATH  = "song_path"
        private const val ARG_SONG_TITLE = "song_title"

        fun newInstance(song: Song) = AddToPlaylistDialog().apply {
            arguments = Bundle().apply {
                putLong(ARG_SONG_ID, song.id)
                putString(ARG_SONG_PATH, song.path)
                putString(ARG_SONG_TITLE, song.displayTitle)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddToPlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val songId = arguments?.getLong(ARG_SONG_ID) ?: return
        val songPath = arguments?.getString(ARG_SONG_PATH) ?: return
        val songTitle = arguments?.getString(ARG_SONG_TITLE) ?: ""
        val viewModel = ViewModelProvider(requireActivity())[MusicViewModel::class.java]

        binding.tvAddToPlaylistTitle.text = "Add \"$songTitle\" to playlist"

        val song = Song(
            id = songId,
            title = songTitle,
            artist = "",
            album = "",
            duration = 0L,
            path = songPath,
            folderName = "",
            folderPath = "",
            size = 0L,
            dateAdded = 0L,
            albumArtUri = null
        )

        val adapter = AddToPlaylistAdapter { playlist ->
            viewModel.addSongToPlaylist(playlist.id, song)
            dismiss()
        }

        binding.recyclerPlaylists.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerPlaylists.adapter = adapter

        viewModel.playlists.observe(viewLifecycleOwner) { playlists ->
            adapter.submitList(playlists)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
