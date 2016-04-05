/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2011  Bartek Przybylski
 *   Copyright (C) 2015 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.synox.android.ui.adapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;

import com.bumptech.glide.Glide;
import com.synox.android.R;
import com.synox.android.lib.common.utils.Log_OC;
import com.synox.android.utils.BitmapUtils;
import com.synox.android.utils.DisplayUtils;
import com.synox.android.utils.MimetypeIconUtil;

/**
 * This Adapter populates a ListView with all files and directories contained
 * in a local directory
 */
public class LocalFileListAdapter extends RecyclerView.Adapter<LocalRecyclerViewHolder> {

    private static final String TAG = LocalFileListAdapter.class.getSimpleName();

    private ArrayList<String> checkedFiles;
    private Context mContext;
    private File mDirectory;
    private File[] mFiles = null;
    
    public LocalFileListAdapter(File directory, Context context) {
        setHasStableIds(true);
        mContext = context;
        swapDirectory(directory);
        checkedFiles = new ArrayList<>();
    }

    @Override
    public long getItemId(int position) {
        if (mFiles == null || mFiles.length <= position)
            return 0;
        try {
            return mFiles[position].hashCode();
        } catch (ArrayIndexOutOfBoundsException aiobe)
        {
            return 0;
        }
    }

    @Override
    public int getItemCount() {
        return mFiles == null ? 0 : mFiles.length;
    }

    public Object getItem(int position) {
        if (mFiles == null || mFiles.length <= position)
            return null;
        if (position <= mFiles.length) {
            return mFiles[position];
        } else {
            return null;
        }
    }

    @Override
    public LocalRecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
        return new LocalRecyclerViewHolder(view, this, mContext);
    }

    @Override
    public void onBindViewHolder(LocalRecyclerViewHolder holder, int position) {

        if (mFiles != null && mFiles.length > position) {
            File file = mFiles[position];
            holder.fileName.setText(file.getName());

            if (!file.isDirectory()) {
                holder.fileIcon.setImageResource(R.drawable.file);
            } else {
                holder.fileIcon.setImageResource(R.drawable.ic_menu_archive);
            }

            if (!file.isDirectory()) {
                holder.fileSizeV.setVisibility(View.VISIBLE);
                holder.fileSizeV.setText(DisplayUtils.bytesToHumanReadable(file.length()));

                holder.lastModV.setVisibility(View.VISIBLE);
                holder.lastModV.setText(DisplayUtils.unixTimeToHumanReadable(file.lastModified()));

                if(getCheckedFiles().contains(file.getAbsolutePath()))
                {
                    holder.checkBoxV.setChecked(true);
                } else
                        {
                    holder.checkBoxV.setChecked(false);
                }

                holder.checkBoxV.setVisibility(View.VISIBLE);

                // get Thumbnail if file is image
                if (BitmapUtils.isImage(file)){
                    Glide.with(mContext).fromFile().load(new File(file.getAbsolutePath())).centerCrop().into(holder.fileIcon);
                    Log_OC.v(TAG, "Executing task to generate a new thumbnail");
                } else {
                    Uri uri = Uri.fromFile(file);
                    ContentResolver cR = mContext.getContentResolver();
                    String mime = cR.getType(uri);
                    holder.fileIcon.setImageResource(MimetypeIconUtil.getFileTypeIconId(mime, file.getName()));
                }

            } else {
                holder.fileSizeV.setVisibility(View.GONE);
                holder.lastModV.setVisibility(View.GONE);
                holder.checkBoxV.setVisibility(View.GONE);
            }

            // not GONE; the alignment changes; ugly way to keep it
            holder.localStateView.setVisibility(View.INVISIBLE);
            holder.favoriteIcon.setVisibility(View.GONE);

            holder.sharedIconV.setVisibility(View.GONE);
            holder.sharedWithMeIconV.setVisibility(View.GONE);
        }

    }

    public ArrayList<String> getCheckedFiles()
    {
        return checkedFiles;
    }

    public void setCheckedFile(String path)
    {
        checkedFiles.add(path);
    }

    public void removeCheckedFile(String path)
    {
        checkedFiles.remove(path);
    }


    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    /**
     * Change the adapted directory for a new one
     * @param directory     New file to adapt. Can be NULL, meaning "no content to adapt".
     */
    public void swapDirectory(File directory) {
        mDirectory = directory;
        mFiles = (mDirectory != null ? mDirectory.listFiles() : null);
        if (mFiles != null) {
            Arrays.sort(mFiles, new Comparator<File>() {
                @Override
                public int compare(File lhs, File rhs) {
                    if (lhs.isDirectory() && !rhs.isDirectory()) {
                        return -1;
                    } else if (!lhs.isDirectory() && rhs.isDirectory()) {
                        return 1;
                    }
                    return compareNames(lhs, rhs);
                }
            
                private int compareNames(File lhs, File rhs) {
                    return lhs.getName().toLowerCase().compareTo(rhs.getName().toLowerCase());                
                }

            });
        }
        notifyDataSetChanged();
    }
}
