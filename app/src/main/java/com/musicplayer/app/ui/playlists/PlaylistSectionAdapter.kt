package com.musicplayer.app.ui.playlists

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.musicplayer.app.databinding.ItemPlaylistHeaderBinding
import com.musicplayer.app.databinding.ItemPlaylistBinding
import com.musicplayer.app.db.PlaylistEntity

class PlaylistSectionAdapter(
    private val onPlaylistClick: (PlaylistEntity) -> Unit,
    private val onPlaylistLongClick: (PlaylistEntity) -> Unit,
    private val onFavouritesClick: () -> Unit,
    private val onRecentlyPlayedClick: () -> Unit,
    private val onMostPlayedClick: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var playlists: List<PlaylistEntity> = emptyList()

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_SPECIAL = 1
        private const val TYPE_PLAYLIST = 2
        private const val SPECIAL_COUNT = 3 // Favourites, Recently Played, Most Played
    }

    fun submitPlaylists(list: List<PlaylistEntity>) {
        playlists = list
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = 1 + SPECIAL_COUNT + playlists.size

    override fun getItemViewType(position: Int): Int = when {
        position == 0 -> TYPE_HEADER
        position <= SPECIAL_COUNT -> TYPE_SPECIAL
        else -> TYPE_PLAYLIST
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderVH(ItemPlaylistHeaderBinding.inflate(inflater, parent, false))
            TYPE_SPECIAL -> SpecialVH(ItemPlaylistBinding.inflate(inflater, parent, false))
            else -> PlaylistVH(ItemPlaylistBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when {
            position == 0 -> (holder as HeaderVH).bind()
            position == 1 -> (holder as SpecialVH).bind("❤️ Favourites") { onFavouritesClick() }
            position == 2 -> (holder as SpecialVH).bind("🕐 Recently Played") { onRecentlyPlayedClick() }
            position == 3 -> (holder as SpecialVH).bind("🔥 Most Played") { onMostPlayedClick() }
            else -> (holder as PlaylistVH).bind(playlists[position - 1 - SPECIAL_COUNT])
        }
    }

    inner class HeaderVH(private val b: ItemPlaylistHeaderBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind() { b.tvHeader.text = "My Playlists" }
    }

    inner class SpecialVH(private val b: ItemPlaylistBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(title: String, onClick: () -> Unit) {
            b.tvPlaylistName.text = title
            b.tvSongCount.text = ""
            b.root.setOnClickListener { onClick() }
        }
    }

    inner class PlaylistVH(private val b: ItemPlaylistBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(playlist: PlaylistEntity) {
            b.tvPlaylistName.text = playlist.name
            b.tvSongCount.text = ""
            b.root.setOnClickListener { onPlaylistClick(playlist) }
            b.root.setOnLongClickListener { onPlaylistLongClick(playlist); true }
        }
    }
}
