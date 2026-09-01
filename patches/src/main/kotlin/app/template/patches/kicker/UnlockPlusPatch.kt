package app.template.patches.kicker

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.template.util.injectEnumReturnByString

internal object GetPlusAboStateFingerprint : Fingerprint(
    definingClass = "KUserImpl;",
    returnType = "KPlusAboState;",
)

@Suppress("unused")
val unlockPlusPatch = bytecodePatch(
    name = "Unlock Plus",
    description = "Unlocks the Plus subscription.",
) {
    // Tested with 7.14.1
    compatibleWith("com.netbiscuits.kicker")
    execute {
        injectEnumReturnByString(GetPlusAboStateFingerprint.method, "PLUS")
    }
}