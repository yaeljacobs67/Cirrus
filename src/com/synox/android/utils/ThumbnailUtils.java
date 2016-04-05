package com.synox.android.utils;

import android.accounts.Account;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.view.Display;
import android.view.WindowManager;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.synox.android.MainApp;
import com.synox.android.R;
import com.synox.android.authentication.AccountUtils;
import com.synox.android.datamodel.OCFile;
import com.synox.android.lib.common.OwnCloudAccount;
import com.synox.android.lib.common.utils.Log_OC;
import com.synox.android.lib.resources.status.OwnCloudVersion;

/**
 * Created by ddijak on 3.11.2015.
 */
public class ThumbnailUtils {
    static{
        init();
    }

    private static Glide thumbnail;
    private static Context mContext;
    private static final float SCALE_DEFAULT = 1f;
    private static final float SCALE_ADAPT = 0.5f;

    /**
     * Init
     */
    private static void init()
    {
        mContext = getContext();
        thumbnail = Glide.get( mContext);
    }

    /**
     * Get context
     */
    private static Context getContext()
    {
        return MainApp.getAppContext();
    }

    /**
     * Dimensions class
     */
    static class Dimensions
    {
        private int width;
        private int height;

        public Dimensions()
        {
            super();
        }

        public Dimensions(int w, int h)
        {
            width = w;
            height = h;
        }

        public void setWidth(int w)
        {
            width = w;
        }

        public void setHeight(int h)
        {
            height = h;
        }

        public int getWidth()
        {
            return width;
        }

        public int getHeight()
        {
            return height;
        }
    }

    /**
     * Generate remote thumbnail url
     *
     * @param oca
     * @param f
     * @return
     */
    private static String getThumbnailURL(OwnCloudAccount oca, OCFile f, OwnCloudVersion ocServerVersion, Dimensions dimensions) {

        if (ocServerVersion.isAfter8Version()) {
            return String.valueOf(oca.getBaseUri() + "" +
                    "/index.php/apps/files/api/v1/thumbnail/" +
                    dimensions.getWidth() + "/" + dimensions.getHeight() + Uri.encode(f.getRemotePath(), "/")) + "?modified=" + f.getModificationTimestamp() + "&rid=" + f.getRemoteId();
        } else {
            return String.valueOf(oca.getBaseUri()) + "/index.php/core/preview.png?file=" + f.getRemotePath() +
                    "&x=" + dimensions.getWidth() + "&y=" + dimensions.getHeight() + "&modified=" + f.getModificationTimestamp() + "&rid=" + f.getRemoteId();
        }
    }

    /**
     * Create Avatar URL
     * @param oca
     * @return
     */
    private static String getAvatarURL(OwnCloudAccount oca) {
        int lastAtPos = oca.getName().lastIndexOf("@");
        String username = oca.getName().substring(0, lastAtPos);
        return String.valueOf(oca.getBaseUri() + "/index.php/avatar/" + username + "/128");
    }

    /**
     * Get gallery image URL
     * @param oca
     * @param f
     * @return
     */
    private static String getGalleryImageURL(OwnCloudAccount oca, OCFile f, Dimensions dimensions)
    {
        return String.valueOf(oca.getBaseUri()) + "/index.php/core/preview.png?file=" + f.getRemotePath() +
                "&x=" + dimensions.getWidth() + "&y=" + dimensions.getHeight() + "&modified=" + f.getModificationTimestamp() + "&rid=" + f.getRemoteId() + "&a=true";
    }

    /**
     * Get thumbnail size
     *
     * @return
     */
    private static int getThumbnailDimension() {
        // Converts dp to pixel
        Resources r = getContext().getResources();
        return Math.round(r.getDimension(R.dimen.file_icon_size_grid));
    }

    /**
     * Main Thumbnail processing
     *
     * @param mAccount account
     * @param file     file
     * @param target   target imageView
     * @param resource file icon
     * @param viewType view type
     * @param processMode 0-thumbnails, 1-gallery images
     */
    public static void processThumbnail(final Account mAccount, final OCFile file, final ImageView target, final int resource, int viewType, int processMode) {

        if (mAccount != null) {
            try {
                final OwnCloudAccount ocAccount = new OwnCloudAccount(mAccount, getContext());
                final OwnCloudVersion serverOCVersion = AccountUtils.getServerVersion(mAccount);

                if (processMode == 0) {
                    int dimensions = getThumbnailDimension();

                    /**
                     * File is a remote image
                     */
                    if (serverOCVersion.supportsRemoteThumbnails() && !file.isDown() && file.isImage()) {
                        thumbnail.with(mContext)
                                .load(getThumbnailURL(ocAccount, file, serverOCVersion, new Dimensions(getThumbnailDimension(),getThumbnailDimension())))
                                .diskCacheStrategy(DiskCacheStrategy.RESULT)
                                .override(dimensions, dimensions)
                                .placeholder(R.drawable.file_image)
                                .fallback(R.drawable.file_image)
                                .centerCrop()
                                .into(target);
                    } else
                    /**
                     * File is a local image or local video
                     */
                        if ((file.isImage() || file.isVideo()) && file.isDown()) {
                            thumbnail.with(mContext)
                                    .load(file.getStoragePath())
                                    .override(dimensions, dimensions)
                                    .placeholder(resource)
                                    .fallback(resource)
                                    .centerCrop()
                                    .into(target);
                        } else
                        /**
                         * Default icon
                         */ {
                            thumbnail.with(mContext)
                                    .load(file.getFileName())
                                    .override(dimensions, dimensions)
                                    .placeholder(resource)
                                    .fallback(resource)
                                    .dontAnimate()
                                    .centerCrop()
                                    .into(target);
                        }
                } else
                if (processMode == 1)
                {
                    if (file.isDown())
                    {
                        // load local gallery image
                        thumbnail.with(mContext)
                                .load(file.getStoragePath())
                                .placeholder(resource)
                                .error(R.drawable.file_image)
                                .skipMemoryCache(true)
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .sizeMultiplier(0.7f)
                                .fitCenter()
                                .into(target);

                    } else {

                        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
                        Display display = wm.getDefaultDisplay();
                        Point size = new Point();
                        display.getSize(size);
                        int width = size.x;
                        int height = size.y;

                        Dimensions d = new Dimensions();
                        d.setWidth(width);
                        d.setHeight(height);


                        thumbnail.with(mContext)
                                .load(getGalleryImageURL(ocAccount, file, d))
                                .asBitmap()
                                .placeholder(resource)
                                .fallback(R.drawable.file_image)
                                .error(R.drawable.file_image)
                                .diskCacheStrategy(DiskCacheStrategy.RESULT)
                                .thumbnail(0.3f)
                                .fitCenter()
                                .into(target);
                    }
                }

            } catch (com.synox.android.lib.common.accounts.AccountUtils.AccountNotFoundException e) {
                Log_OC.e("ThumbnailUtils", e.getMessage());
            }

            /**
             * Default icon in case Account was not found, (should not happen at all!)
             */
        } else {
            target.setImageResource(resource);
        }

        /**
         * Scaling the image
         * 1 - grid view type, 0 - list view type
         */
        if (viewType == 1) {
            if (file.isImage() || (file.isVideo() && file.isDown())) {
                target.setScaleX(SCALE_DEFAULT);
                target.setScaleY(SCALE_DEFAULT);
            } else {
                target.setScaleX(SCALE_ADAPT);
                target.setScaleY(SCALE_ADAPT);
            }
        } else {
            target.setScaleX(SCALE_DEFAULT);
            target.setScaleY(SCALE_DEFAULT);
        }
    }

    /**
     *
     * @param mContext context
     * @param mAccount account
     * @param target   image view target
     * @param resource fallback resource
     */
    public static void processAvatar(final Context mContext, final Account mAccount, final ImageView target, final int resource) {
        try {
            final OwnCloudAccount ocAccount = new OwnCloudAccount(mAccount, mContext);

            final Glide avatar = Glide.get(mContext);
            avatar.with(mContext)
                    .load(getAvatarURL(ocAccount))
                    .placeholder(resource)
                    .fallback(resource)
                    .error(resource)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .centerCrop()
                    .transform(new CircleTransform(mContext))
                    .into(target);
        } catch (com.synox.android.lib.common.accounts.AccountUtils.AccountNotFoundException e) {
            Log_OC.e("ThumbnailUtils", e.getMessage());
        }
    }

    /**
     * Remove all thumbnail data on the device
     */
    public static void clearThumbnailsCache() {
        Glide.get(mContext).clearMemory();
        new Thread(new Runnable() {
            @Override
            public void run() {
                Glide.get(mContext).clearDiskCache();
            }
        }).start();
    }
}