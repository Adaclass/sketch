/*
 * Copyright (C) 2013 Peng fei Pan <sky@xiaopan.me>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.xiaopan.sketch.process;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import me.xiaopan.sketch.Sketch;
import me.xiaopan.sketch.cache.BitmapPool;
import me.xiaopan.sketch.cache.BitmapPoolUtils;
import me.xiaopan.sketch.request.Resize;

/**
 * 用于组合两个ImageProcessor一起使用
 */
public abstract class WrappedImageProcessor extends ResizeImageProcessor {
    private WrappedImageProcessor wrappedProcessor;

    public WrappedImageProcessor(WrappedImageProcessor wrappedProcessor) {
        this.wrappedProcessor = wrappedProcessor;
    }

    @NonNull
    @Override
    public final String getKey() {
        String selfKey = onGetKey();
        String wrappedKey = wrappedProcessor != null ? wrappedProcessor.getKey() : null;
        if (!TextUtils.isEmpty(selfKey)) {
            if (!TextUtils.isEmpty(wrappedKey)) {
                return String.format("%s&%s", selfKey, wrappedKey);
            } else {
                return selfKey;
            }
        } else {
            if (!TextUtils.isEmpty(wrappedKey)) {
                return wrappedKey;
            } else {
                return "WrappedImageProcessor";
            }
        }
    }

    @SuppressWarnings("unused")
    public WrappedImageProcessor getWrappedProcessor() {
        return wrappedProcessor;
    }

    public abstract String onGetKey();

    protected boolean isInterceptResize() {
        return false;
    }

    @NonNull
    @Override
    public final Bitmap process(@NonNull Sketch sketch, @NonNull Bitmap bitmap, @Nullable Resize resize, boolean lowQualityImage) {
        //noinspection ConstantConditions
        if (bitmap == null || bitmap.isRecycled()) {
            return bitmap;
        }

        // resize
        Bitmap newBitmap = bitmap;
        if (!isInterceptResize()) {
            newBitmap = super.process(sketch, bitmap, resize, lowQualityImage);
        }

        // wrapped
        if (wrappedProcessor != null) {
            Bitmap wrappedBitmap = wrappedProcessor.process(sketch, newBitmap, resize, lowQualityImage);
            if (wrappedBitmap != newBitmap) {
                if (newBitmap != bitmap) {
                    BitmapPool bitmapPool = sketch.getConfiguration().getBitmapPool();
                    BitmapPoolUtils.freeBitmapToPool(newBitmap, bitmapPool);
                }
                newBitmap = wrappedBitmap;
            }
        }
        return onProcess(sketch, newBitmap, resize, lowQualityImage);
    }

    @NonNull
    public abstract Bitmap onProcess(@NonNull Sketch sketch, @NonNull Bitmap bitmap, @Nullable Resize resize, boolean lowQualityImage);
}
