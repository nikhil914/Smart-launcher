package com.nikhil.lumen.data

import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager

data class TvInput(val id: String, val label: String, val kind: String) {
    fun intent(): Intent =
        Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(id))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

/** Physical inputs (HDMI, composite...). Empty on streaming sticks, which have none. */
class InputsRepository(private val context: Context) {
    fun loadInputs(): List<TvInput> {
        val manager = context.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager ?: return emptyList()
        return try {
            manager.tvInputList
                .filter { it.isPassthroughInput && !it.isHidden(context) }
                .map { info ->
                    val custom = info.loadCustomLabel(context)?.toString()?.takeIf { it.isNotBlank() }
                    TvInput(
                        id = info.id,
                        label = custom ?: info.loadLabel(context).toString(),
                        kind = kindOf(info.type),
                    )
                }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun kindOf(type: Int): String = when (type) {
        TvInputInfo.TYPE_HDMI -> "HDMI"
        TvInputInfo.TYPE_COMPONENT -> "Component"
        TvInputInfo.TYPE_COMPOSITE -> "Composite"
        TvInputInfo.TYPE_DISPLAY_PORT -> "DisplayPort"
        TvInputInfo.TYPE_DVI -> "DVI"
        TvInputInfo.TYPE_SCART -> "SCART"
        TvInputInfo.TYPE_SVIDEO -> "S-Video"
        TvInputInfo.TYPE_VGA -> "VGA"
        TvInputInfo.TYPE_TUNER -> "Tuner"
        else -> "Input"
    }
}
