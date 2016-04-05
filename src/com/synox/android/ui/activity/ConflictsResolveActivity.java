/**
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author David A. Velasco
 * Copyright (C) 2012 Bartek Przybylski
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

package com.synox.android.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import com.synox.android.datamodel.OCFile;
import com.synox.android.files.services.FileDownloader;
import com.synox.android.files.services.FileUploader;
import com.synox.android.lib.common.utils.Log_OC;
import com.synox.android.ui.dialog.ConflictsResolveDialog;
import com.synox.android.ui.dialog.ConflictsResolveDialog.Decision;
import com.synox.android.ui.dialog.ConflictsResolveDialog.OnConflictDecisionMadeListener;

/**
 * Wrapper activity which will be launched if keep-in-sync file will be modified by external
 * application.
 */
public class ConflictsResolveActivity extends FileActivity implements OnConflictDecisionMadeListener {

    private String TAG = ConflictsResolveActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void conflictDecisionMade(Decision decision) {
        Intent i = new Intent(getApplicationContext(), FileUploader.class);
        OCFile file = getFile();

        switch (decision) {
            case CANCEL:
                finish();
                return;
            case OVERWRITE:
                // use local version -> overwrite on server
                i.putExtra(FileUploader.KEY_FORCE_OVERWRITE, true);
                break;
            case KEEP_BOTH:
                i.putExtra(FileUploader.KEY_LOCAL_BEHAVIOUR, FileUploader.LOCAL_BEHAVIOUR_MOVE);
                break;
            case SERVER:
                // use server version -> delete local, request download
                Intent intent = new Intent(this, FileDownloader.class);
                intent.putExtra(FileDownloader.EXTRA_ACCOUNT, getAccount());
                intent.putExtra(FileDownloader.EXTRA_FILE, file);
                startService(intent);
                finish();
                return;
            default:
                Log_OC.wtf(TAG, "Unhandled conflict decision " + decision);
                return;
        }
        i.putExtra(FileUploader.KEY_ACCOUNT, getAccount());
        i.putExtra(FileUploader.KEY_FILE, file);
        i.putExtra(FileUploader.KEY_UPLOAD_TYPE, FileUploader.UPLOAD_SINGLE_FILE);

        startService(i);
        finish();
    }

    @Override
    protected void onAccountSet(boolean stateWasRecovered) {
        super.onAccountSet(stateWasRecovered);
        if (getAccount() != null) {
            OCFile file = getFile();
            if (file == null) {
                Log_OC.e(TAG, "No conflictive file received");
                finish();
            } else {
                /// Check whether the 'main' OCFile handled by the Activity is contained in the current Account
                file = getStorageManager().getFileByPath(file.getRemotePath());   // file = null if not in the current Account
                if (file != null) {
                    ConflictsResolveDialog d = ConflictsResolveDialog.newInstance(file.getRemotePath(), this);
                    d.showDialog(this);
                } else {
                    // account was changed to a different one - just finish
                    finish();
                }
            }
        } else {
            finish();
        }
    }
}