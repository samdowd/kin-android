// Automatically generated by xdrgen 
// DO NOT EDIT or your changes may be overwritten
package org.kin.stellarfork.xdr

import java.io.IOException

// === xdr source ============================================================
//  typedef opaque UpgradeType<128>;
//  ===========================================================================
class UpgradeType {
    var upgradeType: ByteArray? = null

    companion object {
        @JvmStatic
        @Throws(IOException::class)
        fun encode(stream: XdrDataOutputStream, encodedUpgradeType: UpgradeType) {
            val UpgradeTypesize = encodedUpgradeType.upgradeType!!.size
            stream.writeInt(UpgradeTypesize)
            stream.write(encodedUpgradeType.upgradeType!!, 0, UpgradeTypesize)
        }

        @Throws(IOException::class)
        fun decode(stream: XdrDataInputStream): UpgradeType {
            val decodedUpgradeType = UpgradeType()
            val UpgradeTypesize = stream.readInt()
            decodedUpgradeType.upgradeType = ByteArray(UpgradeTypesize)
            stream.read(decodedUpgradeType.upgradeType!!, 0, UpgradeTypesize)
            return decodedUpgradeType
        }
    }
}
