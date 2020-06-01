package com.example.oneviewapisample.changedefault

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.PatternMatcher
import android.util.Log
import android.widget.Toast
import java.lang.reflect.Method

class ChangeDefaultApplication(context: Context) {
    private var mPackageManager : PackageManager = context.packageManager
    val TAG: String = "ChangeDefaultApp"
    val mContext: Context = context

    /**
     * Get all activities that can be open by this implicit intent
     * */
    fun getAllActivities(intent: Intent) : List<ResolveInfo> {
        return mPackageManager.queryIntentActivities(
            intent,
            PackageManager.MATCH_ALL
        )
    }

    fun getPackageManager() : PackageManager {
        return mPackageManager
    }




    fun changeDefaultApp(packageName:String, intent:Intent)  {

        var resolveInfo : ResolveInfo? = null

        val activitiesList : List<ResolveInfo> = getAllActivities(intent)
        var components = mutableListOf<ComponentName>()

        for(info in activitiesList) {
            if (info.activityInfo.packageName.equals(packageName))
                resolveInfo = info

            components.add(ComponentName(info.activityInfo.packageName, info.activityInfo.name))
        }

        if (resolveInfo != null) {
            val componentName = ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name)
            //create new intent
            var newIntent = intent
            var filter = IntentFilter()
            filter.addAction(Intent.ACTION_VIEW)

            if (intent.categories != null ) {
                for (cat in intent.categories) {
                    filter.addCategory(cat)
                }
            }
            filter.addCategory(Intent.CATEGORY_DEFAULT)

            val cat: Int = resolveInfo!!.match and IntentFilter.MATCH_CATEGORY_MASK
            val data: Uri = newIntent.data!!
            if (cat == IntentFilter.MATCH_CATEGORY_TYPE) {
                val mimeType: String? = newIntent.resolveType(mContext)
                if (mimeType != null) {
                    try {
                        filter.addDataType(mimeType)
                    } catch (e: IntentFilter.MalformedMimeTypeException) {
                        Log.w("ResolverActivity", e)
                    }
                }
            }

            if ( data.scheme != null) {
                // We need the data specification if there was no type,
                // OR if the scheme is not one of our magical "file:"
                // or "content:" schemes (see IntentFilter for the reason).
                if (cat != IntentFilter.MATCH_CATEGORY_TYPE
                    || ("file" != data.scheme
                            && "content" != data.scheme)
                ) {
                    filter.addDataScheme(data.scheme)

                    if (resolveInfo.filter != null) {
                        // Look through the resolved filter to determine which part
                        // of it matched the original Intent.
                        var pIt: Iterator<PatternMatcher> = resolveInfo.filter.schemeSpecificPartsIterator()
                        if (pIt != null) {
                            val ssp = data.schemeSpecificPart
                            while (ssp != null && pIt.hasNext()) {
                                val p: PatternMatcher = pIt.next()
                                if (p.match(ssp)) {
                                    filter.addDataSchemeSpecificPart(p.getPath(), p.getType())
                                    break
                                }
                            }
                        }
                        val aIt: Iterator<IntentFilter.AuthorityEntry> = resolveInfo.filter.authoritiesIterator()
                        if (aIt != null) {
                            while (aIt.hasNext()) {
                                val a: IntentFilter.AuthorityEntry = aIt.next()
                                if (a.match(data) >= 0) {
                                    val port: Int = a.getPort()
                                    filter.addDataAuthority(
                                        a.getHost(),
                                        if (port >= 0) Integer.toString(port) else null
                                    )
                                    break
                                }
                            }
                        }
                        pIt = resolveInfo.filter.pathsIterator()
                        if (pIt != null) {
                            val path = data.path
                            while (path != null && pIt.hasNext()) {
                                val p: PatternMatcher = pIt.next()
                                if (p.match(path)) {
                                    filter.addDataPath(p.getPath(), p.getType())
                                    break
                                }
                            }
                        }

                    }


                }
            }

            //Set default application
            mPackageManager.addPreferredActivity(filter, resolveInfo!!.match,
                components.toTypedArray(), componentName)

            //Set as default browser if is browser
            if(handleAllWebDataURI(resolveInfo)) {
                setDefaultBrowserPackageNameAsUser(resolveInfo.activityInfo.packageName)
            } else {
                // Update Domain Verification status
                val dataScheme = data?.scheme
                val isHttpOrHttps = dataScheme != null &&
                        (dataScheme == "http" || dataScheme == "https")
                val isViewAction = newIntent.action != null && newIntent.action.equals(Intent.ACTION_VIEW)
                val hasCategoryBrowsable = newIntent.categories != null &&
                        newIntent.categories.contains(Intent.CATEGORY_BROWSABLE)

                if (isHttpOrHttps && isViewAction && hasCategoryBrowsable) {
                    updateIntentVerificationStatusAsUser(resolveInfo.activityInfo.packageName)
                }
            }


        }
    }


    @SuppressLint("SoonBlockedPrivateApi")
    fun getUserId() : Int {
        try {
            val m: Method = mPackageManager.javaClass.getDeclaredMethod("getUserId")
            m.isAccessible = true
            return m.invoke(mPackageManager) as Int

        } catch (e: Exception) {
            Log.i(TAG, "Error: "+e.message)
        }
        return 0
    }


    /**
     * return if the {@link IntentFilter} responsible for intent
     * resolution is classified as a "browser".
     */
    private fun handleAllWebDataURI(resolveInfo: ResolveInfo) : Boolean {
        try {
            val handleAllWebDataURI = resolveInfo?.javaClass?.getField("handleAllWebDataURI")
            return handleAllWebDataURI?.getBoolean(resolveInfo)!!
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }

        return false
    }


    /**
     * Set the default Browser package name for a specific user.
     *
     * @param packageName The package name of the default Browser.
     * @param userId The user id.
     *
     * @return true if the default Browser for the specified user has been set,
     *         otherwise return false. If the user id passed is -1 (all users) this call will not
     *         do anything and just return false.
     *
     */
    private fun setDefaultBrowserPackageNameAsUser(packageName:String)  {
        try {
            val m: Method = mPackageManager.javaClass.getDeclaredMethod(
                "setDefaultBrowserPackageNameAsUser",
                String::class.java,
                Int::class.java
            )
            m.isAccessible = true

            m.invoke(mPackageManager,packageName, getUserId())

        } catch (e: Exception) {
            Log.i(TAG, "Error: "+e.message)
        }
    }


/**
 * Allow to change the status of a Intent Verification status for all IntentFilter of an App.
 * This is used by the ResolverActivity to change the status depending on what the User select
 * in the Disambiguation Dialog and also used by the Settings App for changing the default App
 * for a domain.
 *
 * @param packageName The package name of the Activity associated with the IntentFilter.
 * @param status The status to set to. This can be
 *              {@link #INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ASK} or
 *              {@link #INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ALWAYS} or
 *              {@link #INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_NEVER}
 * @param userId The user id.
 *
 * @return true if the status has been set. False otherwise.
 */
    private fun updateIntentVerificationStatusAsUser(packageName: String) : Boolean {
        try {
            val m: Method = mPackageManager.javaClass.getDeclaredMethod(
                "updateIntentVerificationStatusAsUser",
                String::class.java,
                Int::class.java,
                Int::class.java
            )
            m.isAccessible = true

            return m.invoke(packageName, 2, getUserId()) as Boolean //2- INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ALWAYS

        } catch (e: Exception) {
            Log.i(TAG, "Error: "+e.message)
        }

        return false
    }
}