/*
 * Copyright (c) 2013 Menny Even-Danan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.anysoftkeyboard.addons;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.SparseArrayCompat;
import android.util.SparseIntArray;

import com.anysoftkeyboard.utils.Logger;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Locale;

public abstract class AddOnImpl implements AddOn {

    private static final String TAG = "ASK_AddOnImpl";
    private final CharSequence mId;
    private final CharSequence mName;
    private final CharSequence mDescription;
    private final String mPackageName;
    private final Context mAskAppContext;
    private WeakReference<Context> mPackageContext;
    private final int mSortIndex;
    private final AddOnResourceMappingImpl mAddOnResourceMapping;
    private final boolean mHiddenAddOn;

    protected AddOnImpl(Context askContext, Context packageContext, CharSequence id, CharSequence name, CharSequence description, boolean hidden, int sortIndex) {
        mId = id;
        mAskAppContext = askContext;
        mName = name;
        mDescription = description;
        mPackageName = packageContext.getPackageName();
        mPackageContext = new WeakReference<>(packageContext);
        mSortIndex = sortIndex;
        mAddOnResourceMapping = new AddOnResourceMappingImpl(this);
        mHiddenAddOn = hidden;
    }

    public final CharSequence getId() {
        return mId;
    }

    public final CharSequence getDescription() {
        return mDescription;
    }

    public String getPackageName() {
        return mPackageName;
    }

    @Nullable
    public final Context getPackageContext() {
        Context c = mPackageContext.get();
        if (c == null) {
            try {
                c = mAskAppContext.createPackageContext(mPackageName, Context.CONTEXT_IGNORE_SECURITY);
                mPackageContext = new WeakReference<>(c);
            } catch (NameNotFoundException e) {
                Logger.w(TAG, "Failed to find package %s!", mPackageName);
                Logger.w(TAG, "Failed to find package! ", e);
            }
        }
        return c;
    }

    public final int getSortIndex() {
        return mSortIndex;
    }

    public CharSequence getName() {
        return mName;
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AddOn &&
                ((AddOn) o).getId().equals(getId());
    }

    @NonNull
    @Override
    public AddOnResourceMapping getResourceMapping() {
        return mAddOnResourceMapping;
    }

    private static class AddOnResourceMappingImpl implements AddOnResourceMapping {
        private final WeakReference<AddOnImpl> mAddOnWeakReference;
        private final SparseIntArray mAttributesMapping = new SparseIntArray();
        private final SparseArrayCompat<int[]> mStyleableArrayMapping = new SparseArrayCompat<>();

        private AddOnResourceMappingImpl(@NonNull AddOnImpl addOn) {
            mAddOnWeakReference = new WeakReference<>(addOn);
        }

        @Override
        public int[] getRemoteStyleableArrayFromLocal(int[] localStyleableArray) {
            int localStyleableId = Arrays.hashCode(localStyleableArray);
            int indexOfRemoteArray = mStyleableArrayMapping.indexOfKey(localStyleableId);
            if (indexOfRemoteArray >= 0) return mStyleableArrayMapping.valueAt(indexOfRemoteArray);
            AddOnImpl addOn = mAddOnWeakReference.get();
            if (addOn == null) return new int[0];
            Context remoteContext = addOn.getPackageContext();
            if (remoteContext == null) return new int[0];
            int[] remoteAttrIds = Support.createBackwardCompatibleStyleable(localStyleableArray, addOn.mAskAppContext, remoteContext, mAttributesMapping);
            mStyleableArrayMapping.put(localStyleableId, remoteAttrIds);
            return remoteAttrIds;
        }
    }

    /*package*/
    final boolean isHiddenAddon() {
        return mHiddenAddOn;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "%s '%s' from %s (id %s)", getClass().getName(), mName, mPackageName, mId);
    }
}
