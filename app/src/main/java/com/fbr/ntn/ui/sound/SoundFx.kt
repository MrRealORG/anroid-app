package com.fbr.ntn.ui.sound

import android.media.AudioManager
import android.media.ToneGenerator

/** Tiny UI sounds without any audio assets. All calls are safe no-ops on failure.
 *  Sounds are OFF by default and gated by [enabled] (Settings screen). */
object SoundFx {
    var enabled: Boolean = false
    private var generator: ToneGenerator? = null

    private fun gen(): ToneGenerator? {
        if (generator == null) {
            generator = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 45) }.getOrNull()
        }
        return generator
    }

    fun click() {
        if (!enabled) return
        runCatching { gen()?.startTone(ToneGenerator.TONE_PROP_BEEP, 60) }
    }

    fun tab() {
        if (!enabled) return
        runCatching { gen()?.startTone(ToneGenerator.TONE_DTMF_5, 70) }
    }

    fun send() {
        if (!enabled) return
        runCatching { gen()?.startTone(ToneGenerator.TONE_CDMA_ANSWER, 130) }
    }

    fun unlock() {
        if (!enabled) return
        runCatching {
            val g = gen() ?: return
            g.startTone(ToneGenerator.TONE_PROP_ACK, 130)
            Thread {
                runCatching {
                    Thread.sleep(150)
                    gen()?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                }
            }.start()
        }
    }

    fun success() {
        if (!enabled) return
        runCatching { gen()?.startTone(ToneGenerator.TONE_PROP_ACK, 150) }
    }

    fun error() {
        if (!enabled) return
        runCatching { gen()?.startTone(ToneGenerator.TONE_PROP_NACK, 180) }
    }
}
