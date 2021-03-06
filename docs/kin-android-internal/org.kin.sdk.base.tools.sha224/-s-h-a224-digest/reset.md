[kin-android](../../index.md) / [org.kin.sdk.base.tools.sha224](../index.md) / [SHA224Digest](index.md) / [reset](./reset.md)

# reset

`fun reset(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)

reset the chaining variables

`fun reset(other: `[`Memoable`](../-memoable/index.md)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)

Restore a copied object state into this object.

Implementations of this method *should* try to avoid or minimise memory allocation to perform the reset.

### Parameters

`other` - an object originally [copied](#) from an object of the same type as this instance.

### Exceptions

`ClassCastException` - if the provided object is not of the correct type.

`MemoableResetException` - if the **other** parameter is in some other way invalid.