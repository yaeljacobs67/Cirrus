/**
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author masensio
 * @author David A. Velasco
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
package com.synox.android.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.github.clans.fab.FloatingActionMenu;
import com.synox.android.R;
import com.synox.android.datamodel.FileDataStorageManager;
import com.synox.android.datamodel.OCFile;
import com.synox.android.datamodel.StorePosition;
import com.synox.android.lib.common.utils.Log_OC;
import com.synox.android.ui.activity.FileDisplayActivity;
import com.synox.android.ui.activity.FolderPickerActivity;
import com.synox.android.ui.activity.OnEnforceableRefreshListener;
import com.synox.android.ui.adapter.FileListListAdapter;
import com.synox.android.ui.dialog.ConfirmationDialogFragment;
import com.synox.android.ui.dialog.CreateFolderDialogFragment;
import com.synox.android.ui.dialog.FileActionsDialogFragment;
import com.synox.android.ui.dialog.RemoveFileDialogFragment;
import com.synox.android.ui.dialog.RenameFileDialogFragment;
import com.synox.android.ui.dialog.UploadSourceDialogFragment;
import com.synox.android.utils.FileStorageUtils;

import java.io.File;
import java.util.Vector;


/**
 * A Fragment that lists all files and folders in a given path.
 * <p/>
 * TODO refactor to get rid of direct dependency on FileDisplayActivity
 */
public class OCFileListFragment extends ExtendedListFragment implements
        FileActionsDialogFragment.FileActionsDialogFragmentListener,
        SearchView.OnQueryTextListener {

    private static final String TAG = OCFileListFragment.class.getSimpleName();

    private static final String MY_PACKAGE = OCFileListFragment.class.getPackage() != null ?
            OCFileListFragment.class.getPackage().getName() : "com.owncloud.android.ui.fragment";

    public final static String ARG_JUST_FOLDERS = MY_PACKAGE + ".JUST_FOLDERS";
    public final static String ARG_ALLOW_CONTEXTUAL_ACTIONS = MY_PACKAGE + ".ALLOW_CONTEXTUAL";

    private static final String KEY_FILE = MY_PACKAGE + ".extra.FILE";

    private FileFragment.ContainerActivity mContainerActivity;
    private FileListListAdapter mAdapter;
    private boolean mJustFolders;

    private OCFile mFile = null;
    private OCFile mTargetFile;
    private MenuItem searchItem;
    private MenuItem layoutView;
    private SearchView searchView;

    public AsyncTask<OCFile, Void, Vector<OCFile>> asyncLoader;

    private static final String DIALOG_UPLOAD_SOURCE = "DIALOG_UPLOAD_SOURCE";
    private static final String DIALOG_CREATE_FOLDER = "DIALOG_CREATE_FOLDER";

    public FileListListAdapter getAdapter() {
        return mAdapter;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        searchItem = menu.findItem(R.id.action_search);
        layoutView = menu.findItem(R.id.action_changeView);

        // create search bar and search listener
        if (searchItem != null) {
            searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
            searchView.setOnQueryTextListener(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Log_OC.e(TAG, "onAttach");
        getActivity().invalidateOptionsMenu();

        try {
            mContainerActivity = (FileFragment.ContainerActivity) context;
            // Todo REMOVE
            //Navigation.setContainerActivity(mContainerActivity);

        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement " +
                    FileFragment.ContainerActivity.class.getSimpleName());
        }
        try {
            setOnRefreshListener((OnEnforceableRefreshListener) context);

        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement " +
                    SwipeRefreshLayout.OnRefreshListener.class.getSimpleName());
        }
    }


    @Override
    public void onDetach() {
        setOnRefreshListener(null);
        mContainerActivity = null;
        mAdapter = null;
        super.onDetach();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);

        Bundle args = getArguments();
        mJustFolders = (args != null) && args.getBoolean(ARG_JUST_FOLDERS, false);

        final Handler h = new Handler();

        // New file fab on click listener
        newFileFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fabMenu.close(true);
                h.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        UploadSourceDialogFragment dialog = UploadSourceDialogFragment.newInstance(((FileDisplayActivity) getActivity()).getAccount());
                        dialog.show(getActivity().getSupportFragmentManager(), DIALOG_UPLOAD_SOURCE);
                    }
                }, 250);
            }
        });

        // New folder on click listener
        newFolderFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fabMenu.close(true);
                h.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        CreateFolderDialogFragment dialog = CreateFolderDialogFragment.newInstance(getCurrentFile());
                        dialog.show(getActivity().getSupportFragmentManager(), DIALOG_CREATE_FOLDER);
                    }
                }, 250);
            }
        });

        mAdapter = new FileListListAdapter(getContext(), this, mContainerActivity);
        setListAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();

        if (mJustFolders)
        {
            fabMenu.hideMenu(false);
            fabMenu = null;
        }
    }

    public FloatingActionMenu getFab() {
        return fabMenu;
    }

    public ActionBar getSupportAB() throws NullPointerException
    {
        if (getActivity() instanceof FileDisplayActivity) {
            return ((FileDisplayActivity) getActivity()).getSupportActionBar();
        } else
        {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onFileActionChosen(int menuId, int filePosition) {
        mTargetFile = (OCFile) mAdapter.getItem(filePosition);
        switch (menuId) {
            case R.id.action_share_file: {
                mContainerActivity.getFileOperationsHelper().showShareFile(mTargetFile);
                return true;
            }
            case R.id.action_open_file_with: {
                mContainerActivity.getFileOperationsHelper().openFile(mTargetFile);
                return true;
            }
            case R.id.action_rename_file: {
                RenameFileDialogFragment dialog = RenameFileDialogFragment.newInstance(mTargetFile);
                dialog.show(getFragmentManager(), FileDetailFragment.FTAG_RENAME_FILE);
                return true;
            }
            case R.id.action_remove_file: {
                RemoveFileDialogFragment dialog = RemoveFileDialogFragment.newInstance(mTargetFile);
                dialog.show(getFragmentManager(), ConfirmationDialogFragment.FTAG_CONFIRMATION);
                return true;
            }
            case R.id.action_download_file:
            case R.id.action_sync_file: {
                mContainerActivity.getFileOperationsHelper().syncFile(mTargetFile);
                return true;
            }
            case R.id.action_cancel_sync: {
                ((FileDisplayActivity)mContainerActivity).cancelTransference(mTargetFile);
                return true;
            }
            /*case R.id.action_see_details: {
                mContainerActivity.showDetails(mTargetFile);
                return true;
            }*/
            case R.id.action_send_file: {
                // Obtain the file
                if (!mTargetFile.isDown()) {  // Download the file
                    Log_OC.d(TAG, mTargetFile.getRemotePath() + " : File must be downloaded");
                    ((FileDisplayActivity) mContainerActivity).startDownloadForSending(mTargetFile);

                } else {
                    mContainerActivity.getFileOperationsHelper().sendDownloadedFile(mTargetFile);
                }
                return true;
            }
            case R.id.action_move: {
                Intent action = new Intent(getActivity(), FolderPickerActivity.class);

                // Pass mTargetFile that contains info of selected file/folder
                action.putExtra(FolderPickerActivity.EXTRA_FILE, mTargetFile);
                getActivity().startActivityForResult(action, FileDisplayActivity.ACTION_MOVE_FILES);
                return true;
            }
            case R.id.action_favorite_file: {
                mContainerActivity.getFileOperationsHelper().toggleFavorite(mTargetFile, true);
                getAdapter().notifyItemChanged(filePosition);
                return true;
            }
            case R.id.action_unfavorite_file: {
                mContainerActivity.getFileOperationsHelper().toggleFavorite(mTargetFile, false);
                getAdapter().notifyItemChanged(filePosition);
                return true;
            }
            case R.id.action_see_details: {
                mContainerActivity.showDetails(mTargetFile);
                return true;
            }
            case R.id.action_copy:
                Intent action = new Intent(getActivity(), FolderPickerActivity.class);

                // Pass mTargetFile that contains info of selected file/folder
                action.putExtra(FolderPickerActivity.EXTRA_FILE, mTargetFile);
                getActivity().startActivityForResult(action, FileDisplayActivity.ACTION_COPY_FILES);
                return true;
            default:
                return false;
        }
    }

    /**
     * {@inhericDoc}
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {

        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        boolean matched = onFileActionChosen(item.getItemId(), ((AdapterContextMenuInfo) item.getMenuInfo()).position);
        if (!matched) {
            return super.onContextItemSelected(item);
        } else {
            return matched;
        }
    }

    public void sortByName(boolean descending) {
        mAdapter.setSortOrder(FileStorageUtils.SORT_NAME, descending);
    }

    public void sortByDate(boolean descending) {
        mAdapter.setSortOrder(FileStorageUtils.SORT_DATE, descending);
    }

    public void sortBySize(boolean descending) {
        mAdapter.setSortOrder(FileStorageUtils.SORT_SIZE, descending);
    }

    /**
     * Cahnging the layout view , list or grid
     *
     * @param viewMode
     */
    public void layoutView(int viewMode) {

        int layoutMode = 0;

        // save list position
        Parcelable s = getListView().getLayoutManager().onSaveInstanceState();
        StorePosition.setParentPosition(mFile.getFileId(), s);

        switch (viewMode) {
            case 0:
                mLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
                mAdapter.setResourceLayout(R.layout.list_item);
                layoutView.setIcon(R.drawable.ic_grid);
                layoutMode = R.drawable.ic_grid;
                break;
            case 1:
                if (getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    mLayoutManager = new GridLayoutManager(getActivity(), NUMBER_OF_GRID_COLUMNS, GridLayoutManager.VERTICAL, false);
                } else {
                    mLayoutManager = new GridLayoutManager(getActivity(), NUMBER_OF_GRID_COLUMNS_LANDSCAPE, GridLayoutManager.VERTICAL, false);
                }
                mAdapter.setResourceLayout(R.layout.grid_item);
                layoutView.setIcon(R.drawable.ic_list);
                layoutMode = R.drawable.ic_list;
                break;
            default:
                mLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
                mAdapter.setResourceLayout(R.layout.list_item);
                layoutView.setIcon(R.drawable.ic_grid);
                layoutMode = R.drawable.ic_grid;
                break;
        }

        // update current layout mode
        SharedPreferences.Editor editor = mAppPreferences.edit();
        editor.putInt("layoutMode", layoutMode);
        editor.apply();

        mCurrentListView.setLayoutManager(mLayoutManager);

        // restore list position
        if (StorePosition.getListPositionList().containsKey(mFile.getFileId())) {
            Parcelable r = StorePosition.getParentPosition(mFile.getFileId());
            getListView().getLayoutManager().onRestoreInstanceState(r);
            StorePosition.removeParentPosition(mFile.getFileId());
        }

    }

    public RecyclerView.LayoutManager getLayoutManager() {
        return mLayoutManager;
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        searchItem.collapseActionView();
        return false;
    }

    @Override
    public boolean onQueryTextChange(String s) {
        mAdapter.getFilter().filter(s);
        return false;
    }

    /**
     * Use this to query the {@link OCFile} that is currently
     * being displayed by this fragment
     *
     * @return The currently viewed OCFile
     */
    public OCFile getCurrentFile() {
        return mFile;
    }

    public void setCurrentFile(OCFile file) {
        mFile = file;
    }

    public int onBrowseUp() {
        OCFile parentDir;
        int moveCount = 0;

        if (mFile != null) {
            FileDataStorageManager storageManager = mContainerActivity.getStorageManager();

            String parentPath = null;
            if (mFile.getParentId() != FileDataStorageManager.ROOT_PARENT_ID) {
                parentPath = new File(mFile.getRemotePath()).getParent();
                parentPath = parentPath.endsWith(OCFile.PATH_SEPARATOR) ? parentPath :
                        parentPath + OCFile.PATH_SEPARATOR;
                parentDir = storageManager.getFileByPath(parentPath);
                moveCount++;
            } else {
                parentDir = storageManager.getFileByPath(OCFile.ROOT_PATH);
            }
            while (parentDir == null) {
                if (parentPath != null) {
                    parentPath = new File(parentPath).getParent();
                    parentPath = parentPath.endsWith(OCFile.PATH_SEPARATOR) ? parentPath :
                            parentPath + OCFile.PATH_SEPARATOR;
                    parentDir = storageManager.getFileByPath(parentPath);
                    moveCount++;
                }
            }
            mFile = parentDir;
            listDirectory(mFile, false, "onBrowseUp");

            // restore list position
            if (StorePosition.getListPositionList().containsKey(mFile.getFileId())) {
                Parcelable r = StorePosition.getParentPosition(mFile.getFileId());
                getListView().getLayoutManager().onRestoreInstanceState(r);
            }
        }

        return moveCount;
    }

    /**
     * Calls listDirectory with a null parameter
     */
    public void listDirectory() {
        listDirectory(null, false, "null");
    }

    public void refreshDirectory(String event) {
        listDirectory(mFile, false, event);
    }

    /**
     * Lists the given directory on the view. When the input parameter is null,
     * it will either refresh the last known directory. list the root
     * if there never was a directory.
     *
     * @param directory File to be listed
     */

    public void listDirectory(final OCFile directory, final boolean scroll, final String from) {

            asyncLoader = new AsyncTask<OCFile, Void, Vector<OCFile>>() {
                private FileDataStorageManager storageManager;
                private Vector<OCFile> mFetchedFiles;

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    if ((from.equals("onBrowseUp") || from.equals("folderChange")))
                    {
                        try {
                            mAdapter.clearAdapterData(from);
                        } catch (NullPointerException npe) {
                            // nullpointer exception ignore
                        }
                    }

                    // collapse search view if opened
                    if (searchItem != null) {
                        searchItem.collapseActionView();
                    }
                }

                @Override
                protected Vector<OCFile> doInBackground(OCFile... params) {

                    if (isCancelled()) return null;

                    OCFile tmpDirectory = params[0];

                    storageManager = mContainerActivity.getStorageManager();
                    if (storageManager != null) {
                        // Check input parameters for null
                        if (tmpDirectory == null) {
                            if (mFile != null) {
                                tmpDirectory = mFile;
                            } else {
                                tmpDirectory = storageManager.getFileByPath("/");
                                if (tmpDirectory == null) return null; // no files, wait for sync
                            }
                        }

                        // If that's not a directory -> List its parent
                        if (!tmpDirectory.isFolder()) {
                            tmpDirectory = storageManager.getFileById(tmpDirectory.getParentId());
                        }
                        mFile = tmpDirectory;
                    }

                    if (storageManager != null && mAdapter != null) {

                        mFetchedFiles = storageManager.getFolderContent(mFile);
                        if (mFetchedFiles.size() > 0) {

                            if (mJustFolders) {
                                mFetchedFiles = mAdapter.getFolders(mFetchedFiles);
                            }
                        }

                        // sort files
                        mFetchedFiles = FileStorageUtils.sortFolder(mFetchedFiles);
                    }


                    return mFetchedFiles;
                }

                @Override
                protected void onPostExecute(Vector<OCFile> nFileList) {
                    if (!isCancelled()) {
                        // show fab button
                        if (getFab() != null) {
                            getFab().showMenu(true);
                        }

                        // scroll to 0 when we go deeper into the folders
                        if (mFile != null && mFile.getParentId() > 0
                                && !StorePosition.getListPositionList().containsKey(mFile.getFileId())
                                && scroll) {
                            getListView().scrollToPosition(0);
                        }

                        try {
                            mAdapter.fillAdapter(nFileList);
                            getSupportAB().setSubtitle(mAdapter.generateFileCount());
                        } catch (NullPointerException npe) {
                            // nullpointer exception ignore
                        }
                    }
                }
            };
            asyncLoader.execute(directory);
    }
}
