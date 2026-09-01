package app.template.patches.kicker

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.template.util.injectEnumReturnByString

internal object GetPurAboStateFingerprint : Fingerprint(
    definingClass = "KUserImpl;",
    returnType = "KPurAboState;",
)

@Suppress("unused")
val unlockPurPatch = bytecodePatch(
    name = "Unlock Pur",
    description = "Unlocks the Pur subscription.",
) {
    // Tested with 7.14.1
    compatibleWith("com.netbiscuits.kicker")
    execute {
        injectEnumReturnByString(GetPurAboStateFingerprint.method, "PUR")
    }
}