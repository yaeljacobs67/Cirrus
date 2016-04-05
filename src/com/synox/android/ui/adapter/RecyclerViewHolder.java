package com.synox.android.ui.adapter;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.synox.android.R;
import com.synox.android.datamodel.OCFile;
import com.synox.android.datamodel.StorePosition;
import com.synox.android.files.FileMenuFilter;
import com.synox.android.lib.common.utils.Log_OC;
import com.synox.android.ui.activity.FileDisplayActivity;
import com.synox.android.ui.dialog.FileActionsDialogFragment;
import com.synox.android.ui.fragment.FileFragment;
import com.synox.android.ui.fragment.OCFileListFragment;
import com.synox.android.ui.preview.PreviewImageFragment;
import com.synox.android.ui.preview.PreviewMediaFragment;
import com.synox.android.ui.preview.PreviewTextFragment;

/**
 * Created by ddijak on 4.11.2015.
 */
public class RecyclerViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener, View.OnCreateContextMenuListener {

    private static final String TAG = RecyclerViewHolder.class.getSimpleName();

    protected TextView fileName;
    protected ImageView fileIcon;
    protected TextView fileSizeV;
    protected TextView lastModV;
    protected CheckBox checkBoxV;
    protected ImageView favoriteIcon;
    protected ImageView localStateView;
    protected ImageView sharedIconV;
    protected ImageView sharedWithMeIconV;
    protected ImageView sharedWithOthersIconV;
    protected RelativeLayout itemContainer;

    private FileListListAdapter adapter;
    private FileFragment.ContainerActivity mComponentsGetter;
    private OCFileListFragment mListFragment;

    public RecyclerViewHolder(View itemView, FileListListAdapter adapter, FileFragment.ContainerActivity mComponentsGetter, OCFileListFragment listFragment) {
        super(itemView);
        itemView.setOnClickListener(this);
        itemView.setOnLongClickListener(this);
        itemView.setOnCreateContextMenuListener(this);

        this.adapter = adapter;
        this.mComponentsGetter = mComponentsGetter;
        this.mListFragment = listFragment;

        this.fileName = (TextView) itemView.findViewById(R.id.fileName);
        this.fileIcon = (ImageView) itemView.findViewById(R.id.thumbnail);
        this.fileSizeV = (TextView) itemView.findViewById(R.id.file_size);
        this.lastModV = (TextView) itemView.findViewById(R.id.last_mod);
        this.checkBoxV = (CheckBox) itemView.findViewById(R.id.custom_checkbox);
        this.favoriteIcon = (ImageView) itemView.findViewById(R.id.favoriteIcon);
        this.localStateView = (ImageView) itemView.findViewById(R.id.localFileIndicator);
        this.sharedIconV = (ImageView) itemView.findViewById(R.id.sharedIcon);
        this.sharedWithMeIconV = (ImageView) itemView.findViewById(R.id.sharedWithMeIcon);
        this.sharedWithOthersIconV =  (ImageView) itemView.findViewById(R.id.sharedWithOthersIcon);
        this.itemContainer = (RelativeLayout) itemView.findViewById(R.id.item_container);
    }

    @Override
    public void onClick(View v) {

        try {
            final OCFile file = adapter.getCurrentFiles().get(getAdapterPosition());
            mListFragment.setCurrentFile(file);

            if (file != null) {

                // save list position
                Parcelable s = mListFragment.getListView().getLayoutManager().onSaveInstanceState();
                StorePosition.setParentPosition(file.getParentId(), s);

                if (file.isFolder()) {

                    // update state and view of this fragment
                    mListFragment.listDirectory(file, true, "folderChange");
                    // then, notify parent activity to let it update its state and view
                    mComponentsGetter.onBrowsedDownTo(file);

                } else { /// Click on a file
                    if (PreviewImageFragment.canBePreviewed(file)) {
                        // preview image - it handles the download, if needed
                        ((FileDisplayActivity) this.mComponentsGetter).startImagePreview(file);
                    } else if (PreviewTextFragment.canBePreviewed(file)) {
                        ((FileDisplayActivity) this.mComponentsGetter).startTextPreview(file);
                    } else if (file.isDown()) {
                        if (PreviewMediaFragment.canBePreviewed(file)) {
                            // media preview
                            ((FileDisplayActivity) this.mComponentsGetter).startMediaPreview(file, 0, true);
                        } else {
                            this.mComponentsGetter.getFileOperationsHelper().openFile(file);
                        }

                    } else {
                        // automatic download, preview on finish
                        ((FileDisplayActivity) this.mComponentsGetter).startDownloadForPreview(file);
                    }
                }

            } else {
                Log_OC.d(TAG, "Null object in ListAdapter!!");
            }
        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onLongClick(View v) {
        showFileAction(getAdapterPosition());
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {

        Bundle args = mListFragment.getArguments();
        boolean allowContextualActions =
                (args == null) || args.getBoolean(OCFileListFragment.ARG_ALLOW_CONTEXTUAL_ACTIONS, true);
        if (allowContextualActions) {
            MenuInflater inflater = mListFragment.getActivity().getMenuInflater();
            inflater.inflate(R.menu.file_actions_menu, menu);
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            OCFile targetFile = (OCFile) adapter.getItem(info.position);

            if (mComponentsGetter != null && mComponentsGetter.getStorageManager() != null) {
                FileMenuFilter mf = new FileMenuFilter(
                        targetFile,
                        mComponentsGetter.getStorageManager().getAccount(),
                        mComponentsGetter,
                        mListFragment.getActivity()
                );
                mf.filter(menu);
            }
        }
    }

    private void showFileAction(int fileIndex) {
        Bundle args = mListFragment.getArguments();
        PopupMenu pm = new PopupMenu(mListFragment.getActivity(),null);
        Menu menu = pm.getMenu();

        boolean allowContextualActions =
                (args == null) || args.getBoolean(OCFileListFragment.ARG_ALLOW_CONTEXTUAL_ACTIONS, true);

        if (allowContextualActions) {
            MenuInflater inflater = mListFragment.getActivity().getMenuInflater();

            inflater.inflate(R.menu.file_actions_menu, menu);
            OCFile targetFile = (OCFile) adapter.getItem(fileIndex);

            if (mComponentsGetter.getStorageManager() != null) {
                FileMenuFilter mf = new FileMenuFilter(
                        targetFile,
                        mComponentsGetter.getStorageManager().getAccount(),
                        mComponentsGetter,
                        mListFragment.getActivity()
                );
                mf.filter(menu);
            }

            FileActionsDialogFragment dialog = FileActionsDialogFragment.newInstance(menu, fileIndex, targetFile);
            dialog.setTargetFragment(mListFragment, 0);
            dialog.show(mListFragment.getFragmentManager(), FileActionsDialogFragment.FTAG_FILE_ACTIONS);
        }
    }
}
