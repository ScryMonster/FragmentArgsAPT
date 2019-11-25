package com.example.buildapt2

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

@Parcelize
class Card(val number:String,
           val type:String,
           val amount: BigDecimal
) : Parcelable {
}