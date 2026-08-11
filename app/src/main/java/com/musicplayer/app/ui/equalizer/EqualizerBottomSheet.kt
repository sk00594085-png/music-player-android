package com.musicplayer.app.ui.equalizer

import android.media.audiofx.Equalizer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.musicplayer.app.R
import com.musicplayer.app.databinding.BottomsheetEqualizerBinding
import com.musicplayer.app.utils.AppPreferences

class EqualizerBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomsheetEqualizerBinding? = null
    private val binding get() = _binding!!

    companion object {
        var equalizer: Equalizer? = null
        val presets = listOf("Normal", "Rock", "Pop", "Classical", "Jazz", "Bass Boost", "Flat")

        // Preset band levels in milliBels relative to center (5 bands)
        private val presetLevels = mapOf(
            "Normal"     to shortArrayOf(0, 0, 0, 0, 0),
            "Rock"       to shortArrayOf(400, 200, -100, 200, 400),
            "Pop"        to shortArrayOf(-100, 200, 400, 200, -100),
            "Classical"  to shortArrayOf(500, 300, -200, 200, 500),
            "Jazz"       to shortArrayOf(300, 200, 0, 200, 300),
            "Bass Boost" to shortArrayOf(600, 400, 0, 0, 0),
            "Flat"       to shortArrayOf(0, 0, 0, 0, 0)
        )

        fun applyPreset(presetName: String) {
            val levels = presetLevels[presetName] ?: return
            val eq = equalizer ?: return
            val numBands = eq.numberOfBands.toInt()
            for (i in 0 until minOf(numBands, levels.size)) {
                val range = eq.bandLevelRange
                val min = range[0].toInt()
                val max = range[1].toInt()
                val centered = (min + max) / 2
                val clamped = (centered + levels[i].toInt()).coerceIn(min, max).toShort()
                eq.setBandLevel(i.toShort(), clamped)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetEqualizerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val eq = equalizer
        if (eq == null) {
            dismiss()
            return
        }

        // Enable toggle
        val isEnabled = AppPreferences.isEqEnabled(requireContext())
        binding.switchEq.isChecked = isEnabled
        eq.enabled = isEnabled

        binding.switchEq.setOnCheckedChangeListener { _, checked ->
            eq.enabled = checked
            AppPreferences.setEqEnabled(requireContext(), checked)
        }

        // Preset spinner
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, presets)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPresets.adapter = adapter

        val savedPreset = AppPreferences.getEqPreset(requireContext())
        val presetIdx = presets.indexOf(savedPreset).coerceAtLeast(0)
        binding.spinnerPresets.setSelection(presetIdx, false)

        val numBands = eq.numberOfBands.toInt()
        val range = eq.bandLevelRange
        val min = range[0].toInt()
        val max = range[1].toInt()

        val seekBars = listOf(
            binding.seekBand0,
            binding.seekBand1,
            binding.seekBand2,
            binding.seekBand3,
            binding.seekBand4
        )
        val labels = listOf(
            binding.tvBand0,
            binding.tvBand1,
            binding.tvBand2,
            binding.tvBand3,
            binding.tvBand4
        )

        // Setup band seekbars
        for (i in 0 until minOf(numBands, seekBars.size)) {
            val centerFreq = eq.getCenterFreq(i.toShort())
            val hz = centerFreq / 1000
            labels[i].text = if (hz >= 1000) "${hz / 1000}kHz" else "${hz}Hz"
            seekBars[i].max = max - min
            seekBars[i].progress = eq.getBandLevel(i.toShort()) - min
            seekBars[i].setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                val band = i.toShort()
                override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        eq.setBandLevel(band, (progress + min).toShort())
                    }
                }
                override fun onStartTrackingTouch(sb: SeekBar) {}
                override fun onStopTrackingTouch(sb: SeekBar) {}
            })
        }

        binding.spinnerPresets.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, v: View?, pos: Int, id: Long) {
                val preset = presets[pos]
                AppPreferences.setEqPreset(requireContext(), preset)
                applyPreset(preset)
                // Refresh seekbar positions
                for (i in 0 until minOf(numBands, seekBars.size)) {
                    seekBars[i].progress = eq.getBandLevel(i.toShort()) - min
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
