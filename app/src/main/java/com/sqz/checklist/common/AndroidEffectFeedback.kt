package com.sqz.checklist.common

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.SoundEffectConstants
import android.view.View
import androidx.annotation.RequiresApi
import com.sqz.checklist.common.device.isEmulator
import sqz.checklist.common.EffectFeedback

class AndroidEffectFeedback(private val view: View) : EffectFeedback {

    private fun makeClickSound() {
        view.playSoundEffect(SoundEffectConstants.CLICK)
    }

    private fun makeVibrate(
        vibe: VibrationEffect, noVibrator: () -> Unit = { this.makeClickSound() }
    ) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                view.context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            view.context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (!vibrator.hasVibrator() || isEmulator()) {
            noVibrator()
            return
        }
        vibrator.vibrate(vibe)
    }

    private fun makeVibrate(
        createPredefined: Int, noVibrator: () -> Unit = { this.makeClickSound() }
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            noVibrator()
            return
        }
        this.makeVibrate(VibrationEffect.createPredefined(createPredefined))
    }

    @SuppressLint("InlinedApi")
    override fun onTapEffect() {
        this.makeVibrate(VibrationEffect.EFFECT_TICK)
    }

    override fun onClickEffect() {
        this.makeClickSound()
    }

    @SuppressLint("InlinedApi")
    override fun onHeavyClickEffect() {
        this.makeVibrate(createPredefined = VibrationEffect.EFFECT_HEAVY_CLICK) {
            this.makeClickSound()
            this.makeClickSound()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onPressEffect() {
        this.makeVibrate(VibrationEffect.EFFECT_CLICK)
    }

    override fun onDragEffect() {
        //this.makeVibrate(VibrationEffect.createOneShot(12L, 58))
        this.makeVibrate(VibrationEffect.createOneShot(15L, 67))
    }
}
