/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
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
package com.synox.android.ui.preview;

import android.accounts.Account;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.synox.android.R;
import com.synox.android.datamodel.OCFile;
import com.synox.android.files.FileMenuFilter;
import com.synox.android.lib.common.utils.Log_OC;
import com.synox.android.ui.dialog.ConfirmationDialogFragment;
import com.synox.android.ui.dialog.RemoveFileDialogFragment;
import com.synox.android.ui.fragment.FileFragment;
import com.synox.android.utils.ThumbnailUtils;
import third_parties.michaelOrtiz.TouchImageViewCustom;


/**
 * This fragment shows a preview of a downloaded image.
 *
 * Trying to get an instance with a NULL {@link OCFile} will produce an
 * {@link IllegalStateException}.
 * 
 * If the {@link OCFile} passed is not downloaded, an {@link IllegalStateException} is generated on
 * instantiation too.
 */
public class PreviewImageFragment extends FileFragment {

    public static final String EXTRA_FILE = "FILE";

    private static final String ARG_FILE = "FILE";
    private static final String ARG_IGNORE_FIRST = "IGNORE_FIRST";
    private static final String ARG_ACCOUNT = "ACCOUNT";

    private TouchImageViewCustom mImageView;
    private ProgressBar mProgressWheel;
    private static final String TAG = PreviewImageFragment.class.getSimpleName();

    private boolean mIgnoreFirstSavedState;
    private Account mAccount;

    /**
     * Public factory method to create a new fragment that previews an image.
     *
     * Android strongly recommends keep the empty constructor of fragments as the only public
     * constructor, and
     * use {@link #setArguments(Bundle)} to set the needed arguments.
     *
     * This method hides to client objects the need of doing the construction in two steps.
     *
     * @param imageFile                 An {@link OCFile} to preview as an image in the fragment
     * @param ignoreFirstSavedState     Flag to work around an unexpected behaviour of
     *                                  {@link FragmentStatePagerAdapter}
     *                                  ; TODO better solution
     */
    public static PreviewImageFragment newInstance(OCFile imageFile, boolean ignoreFirstSavedState, Account mAccount){
        PreviewImageFragment frag = new PreviewImageFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_FILE, imageFile);
        args.putBoolean(ARG_IGNORE_FIRST, ignoreFirstSavedState);
        args.putParcelable(ARG_ACCOUNT, mAccount);
        frag.setArguments(args);
        return frag;
    }

    /**
     *  Creates an empty fragment for image previews.
     * 
     *  MUST BE KEPT: the system uses it when tries to reinstantiate a fragment automatically
     *  (for instance, when the device is turned a aside).
     * 
     *  DO NOT CALL IT: an {@link OCFile} and {@link Account} must be provided for a successful
     *  construction
     */
    public PreviewImageFragment() {
        mIgnoreFirstSavedState = false;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        setFile((OCFile)args.getParcelable(ARG_FILE));
            // TODO better in super, but needs to check ALL the class extending FileFragment;
            // not right now

        mIgnoreFirstSavedState = args.getBoolean(ARG_IGNORE_FIRST);
        mAccount = args.getParcelable(ARG_ACCOUNT);
        setHasOptionsMenu(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final View view = inflater.inflate(R.layout.preview_image_fragment, container, false);
        mImageView = (TouchImageViewCustom) view.findViewById(R.id.image);
        mImageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ((PreviewImageActivity) getActivity()).toggleFullScreen();
            }

        });
        TextView mMessageView = (TextView)view.findViewById(R.id.message);
        mMessageView.setVisibility(View.GONE);

        mProgressWheel = (ProgressBar) view.findViewById(R.id.progressWheel);
        mProgressWheel.setVisibility(View.VISIBLE);

        return view;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            if (!mIgnoreFirstSavedState) {
                OCFile file = savedInstanceState.getParcelable(PreviewImageFragment.EXTRA_FILE);
                setFile(file);
            } else {
                mIgnoreFirstSavedState = false;
            }
        }

        if (getFile() == null) {
            throw new IllegalStateException("Instanced with a NULL OCFile");
        } else
        {
            ThumbnailUtils.processThumbnail(mAccount, getFile(), mImageView, R.drawable.file_image_gallery, 0, 1);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(PreviewImageFragment.EXTRA_FILE, getFile());
    }

    @Override
    public void onStart() {
        super.onStart();
    }


    @Override
    public void onStop() {
        Log_OC.d(TAG, "onStop starts");
        super.onStop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.file_actions_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if (mContainerActivity != null && mContainerActivity.getStorageManager() != null) {
            // Update the file
            setFile(mContainerActivity.getStorageManager().getFileById(getFile().getFileId()));

            FileMenuFilter mf = new FileMenuFilter(
                getFile(),
                mContainerActivity.getStorageManager().getAccount(),
                mContainerActivity,
                getActivity()
            );
            mf.filter(menu);
        }

        // additional restriction for this fragment 
        // TODO allow renaming in PreviewImageFragment
        MenuItem item = menu.findItem(R.id.action_rename_file);
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }

        // additional restriction for this fragment 
        // TODO allow refresh file in PreviewImageFragment
        item = menu.findItem(R.id.action_sync_file);
        if (item != null) {
            if (getFile().isDown()) {
                item.setVisible(false);
                item.setEnabled(false);
            } else
            {
                item.setVisible(true);
                item.setEnabled(true);
            }
        }

        // additional restriction for this fragment
        // TODO allow refresh file in PreviewImageFragment
        item = menu.findItem(R.id.action_download_file);
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }

        // additional restriction for this fragment
        // TODO allow refresh file in PreviewImageFragment
        item = menu.findItem(R.id.action_send_file);
        if (item != null && !getFile().isDown()) {
            item.setVisible(false);
            item.setEnabled(false);
        }


        // additional restriction for this fragment
        item = menu.findItem(R.id.action_move);
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }

        // additional restriction for this fragment
        item = menu.findItem(R.id.action_copy);
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share_file: {
                mContainerActivity.getFileOperationsHelper().showShareFile(getFile());
                return true;
            }
            case R.id.action_open_file_with: {
                openFile();
                return true;
            }
            case R.id.action_remove_file: {
                RemoveFileDialogFragment dialog = RemoveFileDialogFragment.newInstance(getFile());
                dialog.show(getFragmentManager(), ConfirmationDialogFragment.FTAG_CONFIRMATION);
                return true;
            }
            case R.id.action_see_details: {
                seeDetails();
                return true;
            }
            case R.id.action_send_file: {
                mContainerActivity.getFileOperationsHelper().sendDownloadedFile(getFile());
                return true;
            }
            case R.id.action_sync_file: {
                mContainerActivity.getFileOperationsHelper().syncFile(getFile());
                return true;
            }
            case R.id.action_favorite_file:{
                mContainerActivity.getFileOperationsHelper().toggleFavorite(getFile(), true);
                return true;
            }
            case R.id.action_unfavorite_file:{
                mContainerActivity.getFileOperationsHelper().toggleFavorite(getFile(), false);
                return true;
            }
            default:
                return false;
        }
    }


    private void seeDetails() {
        mContainerActivity.showDetails(getFile());
    }


    @Override
    public void onResume() {
        super.onResume();
    }


    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mProgressWheel.setVisibility(View.GONE);
        super.onDestroy();
    }


    /**
     * Opens the previewed image with an external application.
     */
    private void openFile() {
        mContainerActivity.getFileOperationsHelper().openFile(getFile());
        finish();
    }

    /**
     * Helper method to test if an {@link OCFile} can be passed to a {@link PreviewImageFragment}
     * to be previewed.
     * 
     * @param file      File to test if can be previewed.
     * @return          'True' if the file can be handled by the fragment.
     */
    public static boolean canBePreviewed(OCFile file) {
        return (file != null && file.isImage());
    }


    /**
     * Finishes the preview
     */
    private void finish() {
        Activity container = getActivity();
        container.finish();
    }

    public TouchImageViewCustom getImageView() {
        return mImageView;
    }
}
