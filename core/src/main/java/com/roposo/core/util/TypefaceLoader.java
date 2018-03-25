package com.roposo.core.util;

import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.util.Log;
import android.util.LruCache;

import com.roposo.core.util.ContextHelper;

import java.io.File;

public class TypefaceLoader {

    private static LruCache<String, Typeface> sTypefaceCache = new LruCache<String, Typeface>(12);

    public static Typeface getTypeface(File typefaceFile) {
        String typefaceName = typefaceFile.getName();
        try {
            Typeface typeface = sTypefaceCache.get(typefaceName);
            if (typeface == null) {
                typeface = Typeface.createFromFile(typefaceFile);
                // Cache the Typeface object
                sTypefaceCache.put(typefaceName, typeface);
            }
            return typeface;
        } catch (Exception e) {
            return null;
        }
    }

    public static Typeface getTypeface(String typefaceAssetsPath) {
        AssetManager manager = ContextHelper.getContext().getAssets();

        String[] split = typefaceAssetsPath.split("/");

        String typefaceName = split[split.length - 1];
        //try {
        Typeface typeface = sTypefaceCache.get(typefaceName);
        if (typeface == null) {
            try {
                typeface = Typeface.createFromAsset(ContextHelper.getContext().getAssets(), typefaceAssetsPath);
                // Cache the Typeface object
                sTypefaceCache.put(typefaceName, typeface);
            } catch (Exception e) {
                Log.d("typeface", "typefavename:"+typefaceAssetsPath);
            }


        }
        return typeface;
        //} catch(Exception e) {
        //    return null;
        //}
    }
}
