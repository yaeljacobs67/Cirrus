package com.synox.android.ui.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.synox.android.R;
import com.synox.android.ui.activity.UploadFilesActivity;
import com.synox.android.ui.fragment.LocalFileListFragment;

import java.io.File;

/**
 * Created by ddijak on 4.12.2015.
 */
public class LocalRecyclerViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    protected TextView fileName;
    protected ImageView fileIcon;
    protected TextView fileSizeV;
    protected TextView lastModV;
    protected CheckBox checkBoxV;
    protected ImageView favoriteIcon;
    protected ImageView localStateView;
    protected ImageView sharedIconV;
    protected ImageView sharedWithMeIconV;
    protected RelativeLayout itemContainer;
    protected UploadFilesActivity activity;
    protected LocalFileListAdapter mAdapter;
    private Context mContext;

    public LocalRecyclerViewHolder(View itemView, LocalFileListAdapter adapter, Context context) {
        super(itemView);
        itemView.setOnClickListener(this);

        mContext = context;
        this.fileName = (TextView) itemView.findViewById(R.id.fileName);
        this.fileIcon = (ImageView) itemView.findViewById(R.id.thumbnail);
        this.fileSizeV = (TextView) itemView.findViewById(R.id.file_size);
        this.lastModV = (TextView) itemView.findViewById(R.id.last_mod);
        this.checkBoxV = (CheckBox) itemView.findViewById(R.id.custom_checkbox);
        this.favoriteIcon = (ImageView) itemView.findViewById(R.id.favoriteIcon);
        this.localStateView = (ImageView) itemView.findViewById(R.id.localFileIndicator);
        this.sharedIconV = (ImageView) itemView.findViewById(R.id.sharedIcon);
        this.sharedWithMeIconV = (ImageView) itemView.findViewById(R.id.sharedWithMeIcon);
        this.itemContainer = (RelativeLayout) itemView.findViewById(R.id.item_container);
        this.mAdapter = adapter;

        this.checkBoxV.setClickable(false);
    }

    @Override
    public void onClick(View v) {

        File file = (File) mAdapter.getItem(getAdapterPosition());
        if (file != null) {
            /// Click on a directory
            if (file.isDirectory()) {
                // just local updates
                ((UploadFilesActivity)mContext).onDirectoryClick(file);
                mAdapter.swapDirectory(file);
                // notify the click to container Activity
            } else {
                if (checkBoxV.isChecked())
                {
                    checkBoxV.setChecked(false);
                    mAdapter.removeCheckedFile(file.getAbsolutePath());

                } else
                {
                    checkBoxV.setChecked(true);
                    mAdapter.setCheckedFile(file.getAbsolutePath());
                }
            }
        }
    }
}
