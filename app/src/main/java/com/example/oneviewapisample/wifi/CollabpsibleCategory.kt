package com.example.oneviewapisample.wifi

import android.content.Context
import android.util.AttributeSet


class CollapsibleCategory : androidx.preference.PreferenceCategory {
    private var mCollapsed = true

    constructor(
        context: Context?, attrs: AttributeSet?, defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
    }

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context,
        attrs
    ) {
    }

    constructor(context: Context?) : super(context) {}

//    val preferenceCount: Int
//        get() = if (mCollapsed && shouldShowCollapsePref()) {
//            COLLAPSED_ITEM_COUNT
//        } else {
//            super.getPreferenceCount()
//        }

    val realPreferenceCount: Int
        get() = super.getPreferenceCount()

    /**
     * Only show the collapse preference if the list would be longer than the collapsed list
     * plus the collapse preference itself.
     *
     * @return true if collapse pref should be shown
     */
    fun shouldShowCollapsePref(): Boolean {
        return super.getPreferenceCount() >= COLLAPSED_ITEM_COUNT + 1
    }

    var isCollapsed: Boolean
        get() = mCollapsed
        set(collapsed) {
            mCollapsed = collapsed
            notifyHierarchyChanged()
        }

    companion object {
        private const val COLLAPSED_ITEM_COUNT = 3
    }
}