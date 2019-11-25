package com.example.buildapt2

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.example.fragmentargsannotation.ArgumentedFragment
import com.example.fragmentargsannotation.FragmentParam
import com.example.fragmentargsannotation.TestableAnnotation

@ArgumentedFragment
class WalletFragment : Fragment() {

    var name: String? = null

    var name2: String? = null

    var cardNumbers: Long? = null

    @FragmentParam
    lateinit var card:Card

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

}