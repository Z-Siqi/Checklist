package sqz.checklist.common

interface EffectFeedback {

    /**
     * Expect a tap vibrate/sound.
     */
    fun onTapEffect()

    /**
     * Expect a click sound.
     */
    fun onClickEffect()

    /**
     * Expect a heavy click sound/vibrate.
     */
    fun onHeavyClickEffect()

    /**
     * Expect a press sound/vibrate.
     */
    fun onPressEffect()

    /**
     * Expect a drag sound/vibrate.
     */
    fun onDragEffect()
}
