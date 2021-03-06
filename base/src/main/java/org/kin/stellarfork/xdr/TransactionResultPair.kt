// Automatically generated by xdrgen 
// DO NOT EDIT or your changes may be overwritten
package org.kin.stellarfork.xdr

import java.io.IOException

// === xdr source ============================================================
//  struct TransactionResultPair
//  {
//      Hash transactionHash;
//      TransactionResult result; // result for the transaction
//  };
//  ===========================================================================
class TransactionResultPair {
    var transactionHash: Hash? = null
    var result: TransactionResult? = null

    companion object {
        @JvmStatic
        @Throws(IOException::class)
        fun encode(
            stream: XdrDataOutputStream,
            encodedTransactionResultPair: TransactionResultPair
        ) {
            Hash.encode(
                stream,
                encodedTransactionResultPair.transactionHash!!
            )
            TransactionResult.encode(stream, encodedTransactionResultPair.result!!)
        }

        @JvmStatic
        @Throws(IOException::class)
        fun decode(stream: XdrDataInputStream): TransactionResultPair {
            val decodedTransactionResultPair = TransactionResultPair()
            decodedTransactionResultPair.transactionHash = Hash.decode(stream)
            decodedTransactionResultPair.result = TransactionResult.decode(stream)
            return decodedTransactionResultPair
        }
    }
}
