/**
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author Tobias Kaminsky
 * @author David A. Velasco
 * @author masensio
 * Copyright (C) 2011  Bartek Przybylski
 * Copyright (C) 2015 ownCloud Inc.
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.synox.android.ui.adapter;


import android.accounts.Account;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import com.synox.android.R;
import com.synox.android.authentication.AccountUtils;
import com.synox.android.datamodel.FileDataStorageManager;
import com.synox.android.datamodel.OCFile;
import com.synox.android.files.services.FileDownloader.FileDownloaderBinder;
import com.synox.android.files.services.FileUploader.FileUploaderBinder;
import com.synox.android.services.OperationsService.OperationsServiceBinder;
import com.synox.android.ui.fragment.FileFragment;
import com.synox.android.ui.fragment.OCFileListFragment;
import com.synox.android.utils.DisplayUtils;
import com.synox.android.utils.FileStorageUtils;
import com.synox.android.utils.MimetypeIconUtil;
import com.synox.android.utils.ThumbnailUtils;

import java.util.Date;
import java.util.Vector;

/**
 * This Adapter populates a ListView with all files and folders in an ownCloud
 * instance.
 */
public class FileListListAdapter extends RecyclerView.Adapter<RecyclerViewHolder> implements Filterable {

    private Context mContext;
    private OCFileListFragment mListFragment;
    private Vector<OCFile> mFiles = new Vector<>();
    private Vector<OCFile> mFilesOrig = new Vector<>();
    private int resourceLayout = 0;

    private Account mAccount;
    private FileFragment.ContainerActivity mTransferServiceGetter;
    private SharedPreferences mAppPreferences;

    private static final int TYPE_LIST = 0;
    private static final int TYPE_GRID = 1;

    public FileListListAdapter(Context context, OCFileListFragment listFragment, FileFragment.ContainerActivity transferServiceGetter) {

        setHasStableIds(true);

        mContext = context;
        mListFragment = listFragment;
        mAccount = AccountUtils.getCurrentOwnCloudAccount(mContext);
        mTransferServiceGetter = transferServiceGetter;
        mAppPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        // Read sorting order, default to sort by name ascending
        FileStorageUtils.mSortOrder = mAppPreferences.getInt("sortOrder", 0);
        FileStorageUtils.mSortAscending = mAppPreferences.getBoolean("sortAscending", true);
        resourceLayout = mAppPreferences.getInt("viewLayout", R.layout.list_item);
    }

    public Object getItem(int position) {
        if (mFiles == null || mFiles.size() <= position)
            return null;
        if (position <= mFiles.size()) {
            return mFiles.get(position);
        } else {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        if (mFiles == null || mFiles.size() <= position)
            return 0;
        return mFiles.get(position).getFileId();
    }

    @Override
    public int getItemCount() {
        return (null != mFiles && mFiles.size() > 0 ? mFiles.size() : 0);
    }

    @Override
    public int getItemViewType(int position) {
        if (resourceLayout == R.layout.grid_item) {
            return TYPE_GRID;
        } else
        if (resourceLayout == R.layout.list_item)
        {
            return TYPE_LIST;
        } else
            return super.getItemViewType(position);
    }

    @Override
    public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(resourceLayout, parent, false);
        return new RecyclerViewHolder(view, this, mTransferServiceGetter, mListFragment);
    }

    @Override
    public void onBindViewHolder(RecyclerViewHolder holder, int position) {

        OCFile file = null;
        if (mFiles != null && mFiles.size() > position) {
            file = mFiles.get(position);
        }

        if (file != null /*&& getItemViewType(position) == TYPE_ITEM*/) {

            holder.fileName.setText(file.getFileName());
            if (holder.lastModV != null) {
                holder.lastModV.setVisibility(View.VISIBLE);
                holder.lastModV.setText(showRelativeTimestamp(file));
            }

            if (holder.checkBoxV != null) {
                holder.checkBoxV.setVisibility(View.GONE);
            }

            if (holder.fileSizeV != null) {
                holder.fileSizeV.setVisibility(View.VISIBLE);
                holder.fileSizeV.setText(DisplayUtils.bytesToHumanReadable(file.getFileLength()));
            }

            // sharedIcon
            if (file.isSharedViaLink()) {
                holder.sharedIconV.setVisibility(View.VISIBLE);
            } else {
                holder.sharedIconV.setVisibility(View.GONE);
            }
            // share with me icon
            if (file.isSharedWithMe()) {
                holder.sharedWithMeIconV.setVisibility(View.VISIBLE);
            } else {
                holder.sharedWithMeIconV.setVisibility(View.GONE);
            }

            // share with others icon
            if (file.isSharedWithSharee()) {
                holder.sharedWithOthersIconV.setVisibility(View.VISIBLE);
            } else {
                holder.sharedWithOthersIconV.setVisibility(View.GONE);
            }

            // local state
            FileDownloaderBinder downloaderBinder = mTransferServiceGetter.getFileDownloaderBinder();
            FileUploaderBinder uploaderBinder = mTransferServiceGetter.getFileUploaderBinder();
            OperationsServiceBinder opsBinder = mTransferServiceGetter.getOperationsServiceBinder();

            boolean downloading = (downloaderBinder != null && downloaderBinder.isDownloading(mAccount, file));

            downloading |= (opsBinder != null && opsBinder.isSynchronizing(mAccount, file.getRemotePath()));
            if (downloading) {
                holder.localStateView.setImageResource(R.drawable.downloading_file_indicator);
                holder.localStateView.setVisibility(View.VISIBLE);
            } else if (uploaderBinder != null && uploaderBinder.isUploading(mAccount, file)) {
                holder.localStateView.setImageResource(R.drawable.uploading_file_indicator);
                holder.localStateView.setVisibility(View.VISIBLE);
            } else if (file.isDown()) {
                holder.localStateView.setImageResource(R.drawable.local_file_indicator);
                holder.localStateView.setVisibility(View.VISIBLE);
            } else {
                holder.localStateView.setVisibility(View.GONE);
            }

            // this if-else is needed even though favorite icon is visible by default
            // because android reuses views in listview
            if (!file.isFavorite()) {
                holder.favoriteIcon.setVisibility(View.GONE);
            } else {
                holder.favoriteIcon.setVisibility(View.VISIBLE);
            }

            // Icons and thumbnail utils
            ThumbnailUtils.processThumbnail(mAccount,
                    file,
                    holder.fileIcon,
                    file.isFolder() ? MimetypeIconUtil.getFolderTypeIconId(file) :
                            MimetypeIconUtil.getFileTypeIconId(file.getMimetype(), file.getFileName()),
                    getItemViewType(position),
                    0);
        }
    }

    /**
     * Generate file count text for how many folders and files are there in the current location
     *
     * @return generated string of folder and file counts
     */
    public String generateFileCount() {

        int filesCount = 0;
        int foldersCount = 0;
        for (OCFile f : mFiles) {
            if (f.isFolder()) {
                foldersCount++;
            } else {
                filesCount++;
            }
        }

        StringBuilder nResourcesString = new StringBuilder();
        if (foldersCount > 0) {
            nResourcesString.append(foldersCount)
                    .append(" ")
                    .append(mContext.getResources().getQuantityString(R.plurals.folder_resources, foldersCount, foldersCount));
        }
        if (filesCount > 0 && foldersCount > 0) {
            nResourcesString.append(", ");
        }
        if (filesCount > 0) {
            nResourcesString.append(filesCount)
                    .append(" ")
                    .append(mContext.getResources().getQuantityString(R.plurals.file_resources, filesCount, filesCount));
        }

        return nResourcesString.toString();
    }

    /**
     * Return list of current files
     *
     * @return Vector of OCFiles
     */
    public Vector<OCFile> getCurrentFiles() {
        return mFiles;
    }

    /**
     * Clear current adapter data
     */
    public void clearAdapterData(String requestFrom)
    {
        // clear adapter only once
        if (mFiles != null) {
            mFiles.clear();
            if (requestFrom.equals("onBrowseUp")) {
                notifyDataSetChanged();
            }
        }
    }

    public void fillAdapter(Vector<OCFile> nFiles)
    {
        // get current working account
        mAccount = AccountUtils.getCurrentOwnCloudAccount(mContext);

        if (mFiles != null && mFiles.size() > 0) {

            mFiles.clear();
            mFiles.addAll(nFiles);

        } else
        {
            mFiles = nFiles;
        }

        if (mFiles != null && mFiles.size() > 0) {

            if (mFilesOrig != null) {
                mFilesOrig.clear();
            }

            assert mFilesOrig != null;
            mFilesOrig.addAll(mFiles);
        }

        // Notifiy adapter of data changes
        notifyDataSetChanged();
    }


    /**
     * Filter for getting only the folders
     *
     * @param files list of files
     * @return Vector<OCFile>
     */
    public Vector<OCFile> getFolders(Vector<OCFile> files) {
        Vector<OCFile> ret = new Vector<>();
        for (OCFile current : files) {
            if (current.isFolder()) {
                ret.add(current);
            }
        }
        return ret;
    }

    public void setSortOrder(Integer order, boolean ascending) {
        SharedPreferences.Editor editor = mAppPreferences.edit();
        editor.putInt("sortOrder", order);
        editor.putBoolean("sortAscending", ascending);
        editor.apply();

        FileStorageUtils.mSortOrder = order;
        FileStorageUtils.mSortAscending = ascending;

        mFiles = FileStorageUtils.sortFolder(mFiles);
        notifyDataSetChanged();

    }

    private CharSequence showRelativeTimestamp(OCFile file) {

        return DateUtils.getRelativeTimeSpanString(file.getModificationTimestamp(), new Date().getTime(), DateUtils.SECOND_IN_MILLIS);

        /*return DisplayUtils.getRelativeDateTimeString(mContext, file.getModificationTimestamp(),
                DateUtils.SECOND_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0);*/
    }

    public void setResourceLayout(int resLayout) {
        resourceLayout = resLayout;
        SharedPreferences.Editor editor = mAppPreferences.edit();
        editor.putInt("viewLayout", resLayout);
        editor.apply();
    }

    @Override
    public Filter getFilter() {
        // return a filter that filters data based on a constraint

        return new Filter() {

            private int searchQueryLength = 0;

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {

                // if we have deleted a search character,
                // reset data that we are searching on
                if (searchQueryLength < constraint.length()) {
                    searchQueryLength = constraint.length();
                } else {
                    mFiles = mFilesOrig;
                }

                // Create a FilterResults object
                FilterResults results = new FilterResults();

                // If the constraint (search string/pattern) is null
                // or its length is 0, i.e., its empty then
                // we just set the `values` property to the
                // original contacts list which contains all of them
                if (constraint == null || constraint.length() == 0) {
                    results.values = mFilesOrig;
                    results.count = mFilesOrig.size();
                } else {
                    // Some search constraint has been passed
                    // so let's filter accordingly
                    Vector<OCFile> filteredProps = new Vector<>();

                    // We'll go through all the contacts and see
                    // if they contain the supplied string
                    for (OCFile filteredFile : mFilesOrig) {
                        if (filteredFile.getFileName().toUpperCase().contains(constraint.toString().toUpperCase()) |
                                filteredFile.getMimetype().toUpperCase().contains(constraint.toString().toUpperCase())) {
                            // if `contains` == true then add it
                            // to our filtered list
                            filteredProps.add(filteredFile);
                        }
                    }

                    // Finally set the filtered values and size/count
                    results.values = filteredProps;
                    results.count = filteredProps.size();
                }
                // Return our FilterResults object
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                mFiles = (Vector<OCFile>) results.values;
                notifyDataSetChanged();
            }
        };
    }
}
