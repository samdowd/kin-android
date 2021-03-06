// Automatically generated by xdrgen 
// DO NOT EDIT or your changes may be overwritten
package org.kin.stellarfork.xdr

import java.io.IOException

// === xdr source ============================================================
//  struct DataEntry
//  {
//      AccountID accountID; // account this data belongs to
//      string64 dataName;
//      DataValue dataValue;
//
//      // reserved for future use
//      union switch (int v)
//      {
//      case 0:
//          void;
//      }
//      ext;
//  };
//  ===========================================================================
class DataEntry {
    var accountID: AccountID? = null
    var dataName: String64? = null
    var dataValue: DataValue? = null
    var ext: DataEntryExt? = null

    class DataEntryExt {
        var discriminant: Int? = null

        companion object {
            @JvmStatic
            @Throws(IOException::class)
            fun encode(
                stream: XdrDataOutputStream,
                encodedDataEntryExt: DataEntryExt
            ) {
                stream.writeInt(encodedDataEntryExt.discriminant!!.toInt())
                when (encodedDataEntryExt.discriminant) {
                    0 -> {
                    }
                }
            }

            @JvmStatic
            @Throws(IOException::class)
            fun decode(stream: XdrDataInputStream): DataEntryExt {
                val decodedDataEntryExt = DataEntryExt()
                val discriminant = stream.readInt()
                decodedDataEntryExt.discriminant = discriminant
                when (decodedDataEntryExt.discriminant) {
                    0 -> {
                    }
                }
                return decodedDataEntryExt
            }
        }
    }

    companion object {
        @JvmStatic
        @Throws(IOException::class)
        fun encode(stream: XdrDataOutputStream, encodedDataEntry: DataEntry) {
            AccountID.encode(stream, encodedDataEntry.accountID!!)
            String64.encode(stream, encodedDataEntry.dataName!!)
            DataValue.encode(stream, encodedDataEntry.dataValue!!)
            DataEntryExt.encode(stream, encodedDataEntry.ext!!)
        }

        @JvmStatic
        @Throws(IOException::class)
        fun decode(stream: XdrDataInputStream): DataEntry {
            val decodedDataEntry = DataEntry()
            decodedDataEntry.accountID = AccountID.decode(stream)
            decodedDataEntry.dataName = String64.decode(stream)
            decodedDataEntry.dataValue = DataValue.decode(stream)
            decodedDataEntry.ext = DataEntryExt.decode(stream)
            return decodedDataEntry
        }
    }
}
