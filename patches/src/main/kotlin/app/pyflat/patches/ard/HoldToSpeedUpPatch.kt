package app.pyflat.patches.ard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.floatSliderOption
import app.morphe.patcher.patch.intSliderOption
import app.morphe.patcher.patch.resourcePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import org.w3c.dom.Element

private const val EXTENSION_CLASS = "Lapp/pyflat/extension/ard/HoldToSpeedUpPatch;"

internal val COMPATIBILITY_ARD = Compatibility(
    name = "ARD Mediathek",
    packageName = "de.swr.avp.ard",
    apkFileType = ApkFileType.APKM,
    appIconColor = 0x003D8F,
    targets = listOf(
        AppTarget(version = "12.4.1"),
    ),
)

// de.swr.ardplayer.lib.a, the player view that creates the controls WebView.
internal object PlayerViewConstructorFingerprint : Fingerprint(
    name = "<init>",
    parameters = listOf("Landroid/content/Context;"),
    strings = listOf("createWebView"),
)

// Needed for the haptic feedback, the app does not declare it.
private val vibratePermissionPatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { document ->
            val manifest = document.getElementsByTagName("manifest").item(0) as Element
            val usesPermissions = document.getElementsByTagName("uses-permission")
            val alreadyDeclared = (0 until usesPermissions.length).any { index ->
                (usesPermissions.item(index) as Element).getAttribute("android:name") == VIBRATE_PERMISSION
            }
            if (alreadyDeclared) return@use

            manifest.appendChild(
                document.createElement("uses-permission").apply {
                    setAttribute("android:name", VIBRATE_PERMISSION)
                },
            )
        }
    }
}

private const val VIBRATE_PERMISSION = "android.permission.VIBRATE"

@Suppress("unused")
val holdToSpeedUpPatch = bytecodePatch(
    name = "Hold to speed up",
    description = "Adds a YouTube-like gesture: hold the video to temporarily play it faster.",
) {
    compatibleWith(COMPATIBILITY_ARD)
    dependsOn(vibratePermissionPatch)
    extendWith("extensions/extension.mpe")

    val speed by floatSliderOption(
        key = "speed",
        min = 1.25f,
        max = 4f,
        default = 2f,
        step = 0.25f,
        title = "Speed",
        description = "Playback speed while holding the video.",
        required = true,
    )

    val holdDelay by intSliderOption(
        key = "holdDelay",
        min = 200,
        max = 1500,
        default = 400,
        step = 50,
        title = "Hold delay",
        description = "Milliseconds the video must be held before it speeds up.",
        required = true,
    )

    execute {
        PlayerViewConstructorFingerprint.method.apply {
            // Right after the WebView is stored in its field.
            val index = implementation!!.instructions.indexOfFirst { instruction ->
                instruction.opcode == Opcode.IPUT_OBJECT &&
                    ((instruction as ReferenceInstruction).reference as FieldReference).type ==
                    "Landroid/webkit/WebView;"
            }
            if (index < 0) throw PatchException("WebView field assignment not found")

            val webViewRegister = (implementation!!.instructions.elementAt(index) as TwoRegisterInstruction).registerA

            addInstruction(
                index + 1,
                "invoke-static { v$webViewRegister }, $EXTENSION_CLASS->install(Landroid/webkit/WebView;)V",
            )
        }

        mutableClassDefBy(EXTENSION_CLASS).methods.apply {
            first { it.name == "getSpeed" }.addInstructions(
                0,
                """
                    const v0, ${speed!!.toRawBits()}
                    return v0
                """,
            )
            first { it.name == "getHoldDelayMs" }.addInstructions(
                0,
                """
                    const-wide v0, ${holdDelay!!.toLong()}L
                    return-wide v0
                """,
            )
        }
    }
}
