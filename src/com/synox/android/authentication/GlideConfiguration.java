package com.synox.android.authentication;

import android.accounts.Account;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GenericLoaderFactory;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.synox.android.MainApp;
import com.synox.android.lib.common.OwnCloudAccount;
import com.synox.android.lib.common.OwnCloudClient;
import com.synox.android.lib.common.OwnCloudClientManagerFactory;
import com.synox.android.lib.common.utils.Log_OC;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by ddijak on 26.01.2016.
 */
public class GlideConfiguration implements com.bumptech.glide.module.GlideModule {

    @Override
    public void applyOptions(Context context, GlideBuilder builder) {
        // Apply options to the builder here.
        builder.setDecodeFormat(DecodeFormat.PREFER_ARGB_8888);
    }

    @Override
    public void registerComponents(Context context, Glide glide) {
        glide.register(GlideUrl.class, InputStream.class, new SynoxHttpUrlLoader.Factory());
    }
}

class SynoxHttpUrlLoader implements ModelLoader<GlideUrl, InputStream> {

    /**
     * The default factory for {@link SynoxHttpUrlLoader}s.
     */
    public static class Factory implements ModelLoaderFactory<GlideUrl, InputStream> {
        @Override
        public ModelLoader<GlideUrl, InputStream> build(Context context, GenericLoaderFactory factories) {
            return new SynoxHttpUrlLoader();
        }

        @Override
        public void teardown() {
            // Do nothing, this instance doesn't own the client.
        }
    }

    @Override
    public DataFetcher<InputStream> getResourceFetcher(GlideUrl model, int width, int height) {
        return new SynoxStreamFetcher(model);
    }
}

class SynoxStreamFetcher implements DataFetcher<InputStream> {
    private final GlideUrl url;
    private InputStream stream;
    private GetMethod get = null;
    private OwnCloudClient client = null;

    public SynoxStreamFetcher(GlideUrl url) {
        this.url = url;
    }

    @Override
    public InputStream loadData(Priority priority) throws Exception {

        if (client == null) {
            Thread t = new Thread(new Runnable() {
                public void run() {

                    synchronized (SynoxHttpUrlLoader.Factory.class) {
                        try {
                            Account mAccount = AccountUtils.getCurrentOwnCloudAccount(MainApp.getAppContext());
                            OwnCloudAccount ocAccount = new OwnCloudAccount(mAccount,
                                    MainApp.getAppContext());

                            client = OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(ocAccount, MainApp.getAppContext());
                        } catch (com.synox.android.lib.common.accounts.AccountUtils.AccountNotFoundException e) {
                            e.printStackTrace();
                        } catch (AuthenticatorException | OperationCanceledException | IOException e) {
                            Log_OC.e(this.getClass().getName(), e.getMessage());
                        }
                    }
                }

            });
            t.start();
            t.join();
        }

        try {
            get = new GetMethod(url.toStringUrl());
            int status = client.executeMethod(get);
            if (status == HttpStatus.SC_OK) {
                stream = get.getResponseBodyAsStream();
            } else {
                client.exhaustResponse(get.getResponseBodyAsStream());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Return thumb stream
        return stream;
    }

    @Override
    public void cleanup() {

        if (get != null) {
            get.releaseConnection();
        }
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                // Ignored
            }
        }
    }

    @Override
    public String getId() {
        return url.getCacheKey();
    }

    @Override
    public void cancel() {
        // TODO: call cancel on the client when this method is called on a background thread. See #257
    }
}