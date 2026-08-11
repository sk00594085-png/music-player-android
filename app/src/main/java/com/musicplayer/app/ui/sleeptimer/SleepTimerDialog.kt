package com.musicplayer.app.ui.sleeptimer

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.musicplayer.app.R
import com.musicplayer.app.databinding.DialogSleepTimerBinding
import com.musicplayer.app.service.MusicService

class SleepTimerDialog : DialogFragment() {

    private var _binding: DialogSleepTimerBinding? = null
    private val binding get() = _binding!!

    companion object {
        private var countDownTimer: CountDownTimer? = null
        var remainingMs: Long = 0L
            private set

        fun isRunning() = remainingMs > 0L

        fun cancel() {
            countDownTimer?.cancel()
            remainingMs = 0L
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogSleepTimerBinding.inflate(layoutInflater)

        binding.seekbarCustom.max = 120
        binding.seekbarCustom.progress = 30
        binding.tvCustomMinutes.text = getString(R.string.sleep_timer_minutes, 30)

        binding.seekbarCustom.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                val mins = if (progress < 1) 1 else progress
                binding.tvCustomMinutes.text = getString(R.string.sleep_timer_minutes, mins)
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        val presetIds = listOf(
            binding.btn15 to 15,
            binding.btn30 to 30,
            binding.btn45 to 45,
            binding.btn60 to 60,
            binding.btn90 to 90
        )

        for ((btn, mins) in presetIds) {
            btn.setOnClickListener {
                startTimer(mins.toLong())
                dismiss()
            }
        }

        if (isRunning()) {
            binding.btnCancelTimer.isEnabled = true
        }

        binding.btnCancelTimer.setOnClickListener {
            cancel()
            Toast.makeText(requireContext(), R.string.sleep_timer_cancelled, Toast.LENGTH_SHORT).show()
            dismiss()
        }

        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.sleep_timer)
            .setView(binding.root)
            .setPositiveButton(R.string.sleep_timer_set_custom) { _, _ ->
                val mins = if (binding.seekbarCustom.progress < 1) 1 else binding.seekbarCustom.progress.toLong()
                startTimer(mins)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    private fun startTimer(minutes: Long) {
        cancel()
        val ms = minutes * 60 * 1000L
        remainingMs = ms
        countDownTimer = object : CountDownTimer(ms, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                remainingMs = millisUntilFinished
            }
            override fun onFinish() {
                remainingMs = 0L
                // Stop music playback
                val ctx = requireContext().applicationContext
                val intent = Intent(ctx, MusicService::class.java).apply {
                    action = MusicService.ACTION_PAUSE
                }
                ctx.startService(intent)
            }
        }.start()

        val mins = minutes.toInt()
        Toast.makeText(requireContext(), getString(R.string.sleep_timer_set, mins), Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
