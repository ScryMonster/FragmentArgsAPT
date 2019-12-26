package com.example.buildapt2

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.example.fragmentargsannotation.ArgumentedFragment
import com.example.fragmentargsannotation.FragmentParam

@ArgumentedFragment
class WalletFragment : Fragment() {

    var name: String? = null

    var name2: String? = null

    @FragmentParam
    var cardNumbers: Int? = null

    @FragmentParam
    var cardNumber2s: Long? = null

    @FragmentParam
    lateinit var card:Card

    @FragmentParam
    lateinit var test:Test

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        generatedCard
    }

}