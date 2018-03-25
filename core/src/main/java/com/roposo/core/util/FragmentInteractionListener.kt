package com.roposo.core.util

/**
 * @author muddassir on 2/16/18.
 */

interface FragmentInteractionListener {
    fun onPreviousClick(fragmentId: Int, vararg data: Any?) {}
    fun onNextClick(fragmentId: Int, vararg data: Any?) {}
}
