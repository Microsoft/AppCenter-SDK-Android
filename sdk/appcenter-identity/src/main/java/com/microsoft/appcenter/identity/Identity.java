package com.microsoft.appcenter.identity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.annotation.VisibleForTesting;
import android.support.annotation.WorkerThread;

import com.microsoft.appcenter.AbstractAppCenterService;
import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpException;
import com.microsoft.appcenter.http.HttpUtils;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.identity.storage.AuthTokenStorage;
import com.microsoft.appcenter.identity.storage.TokenStorageFactory;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.HandlerUtils;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.async.DefaultAppCenterFuture;
import com.microsoft.appcenter.utils.storage.FileManager;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;

import static android.util.Log.VERBOSE;
import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_GET;
import static com.microsoft.appcenter.http.HttpUtils.createHttpClient;
import static com.microsoft.appcenter.identity.Constants.AUTHORITIES;
import static com.microsoft.appcenter.identity.Constants.AUTHORITY_DEFAULT;
import static com.microsoft.appcenter.identity.Constants.AUTHORITY_TYPE;
import static com.microsoft.appcenter.identity.Constants.AUTHORITY_TYPE_B2C;
import static com.microsoft.appcenter.identity.Constants.AUTHORITY_URL;
import static com.microsoft.appcenter.identity.Constants.CONFIG_URL;
import static com.microsoft.appcenter.identity.Constants.FILE_PATH;
import static com.microsoft.appcenter.identity.Constants.HEADER_E_TAG;
import static com.microsoft.appcenter.identity.Constants.HEADER_IF_NONE_MATCH;
import static com.microsoft.appcenter.identity.Constants.IDENTITY_GROUP;
import static com.microsoft.appcenter.identity.Constants.IDENTITY_SCOPE;
import static com.microsoft.appcenter.identity.Constants.LOG_TAG;
import static com.microsoft.appcenter.identity.Constants.PREFERENCE_E_TAG_KEY;
import static com.microsoft.appcenter.identity.Constants.SERVICE_NAME;

/**
 * Identity service.
 */
public class Identity extends AbstractAppCenterService {

    /**
     * Shared instance.
     */
    @SuppressLint("StaticFieldLeak")
    private static Identity sInstance;

    /**
     * Application context.
     */
    private Context mContext;

    /**
     * Application secret.
     */
    private String mAppSecret;

    /**
     * Authentication client.
     */
    private PublicClientApplication mAuthenticationClient;

    /**
     * Scope we need to use when acquiring user ID tokens.
     */
    private String mIdentityScope;

    /**
     * HTTP client call, for cancellation.
     */
    private ServiceCall mGetConfigCall;

    /**
     * Current activity.
     */
    private Activity mActivity;

    /**
     * True if sign-in was delayed because called in background or configuration not ready.
     */
    private boolean mSignInDelayed;

    /**
     * Not null if sign-in is pending.
     */
    private DefaultAppCenterFuture<SignInResult> mPendingSignInFuture;

    /**
     * Instance of {@link AuthTokenStorage} to store token information.
     */
    private AuthTokenStorage mTokenStorage;

    /**
     * Get shared instance.
     *
     * @return shared instance.
     */
    @SuppressWarnings("WeakerAccess")
    public static synchronized Identity getInstance() {
        if (sInstance == null) {
            sInstance = new Identity();
        }
        return sInstance;
    }

    @VisibleForTesting
    static synchronized void unsetInstance() {
        sInstance = null;
    }

    /**
     * Check whether Identity service is enabled or not.
     *
     * @return future with result being <code>true</code> if enabled, <code>false</code> otherwise.
     * @see AppCenterFuture
     */
    @SuppressWarnings({"unused", "WeakerAccess"}) // TODO Remove warning suppress after release.
    public static AppCenterFuture<Boolean> isEnabled() {
        return getInstance().isInstanceEnabledAsync();
    }

    /**
     * Enable or disable Identity service.
     *
     * @param enabled <code>true</code> to enable, <code>false</code> to disable.
     * @return future with null result to monitor when the operation completes.
     */
    @SuppressWarnings({"unused", "WeakerAccess"}) // TODO Remove warning suppress after release.
    public static AppCenterFuture<Void> setEnabled(boolean enabled) {
        return getInstance().setInstanceEnabledAsync(enabled);
    }

    /**
     * Sign in to get user information.
     *
     * @return future with the result of the asynchronous sign-in operation.
     */
    public static AppCenterFuture<SignInResult> signIn() {
        return getInstance().instanceSignIn();
    }

    /**
     * Sign out user and invalidate a user's token.
     */
    @SuppressWarnings("WeakerAccess")
    // TODO remove warning once jCenter published and reflection removed in test app
    public static void signOut() {
        getInstance().instanceSignOut();
    }

    @Override
    public synchronized void onStarted(@NonNull Context context, @NonNull Channel channel, String appSecret, String transmissionTargetToken, boolean startedFromApp) {
        mContext = context;
        mAppSecret = appSecret;
        mTokenStorage = TokenStorageFactory.getTokenStorage(context);
        super.onStarted(context, channel, appSecret, transmissionTargetToken, startedFromApp);
    }

    /**
     * React to enable state change.
     *
     * @param enabled current state.
     */
    @Override
    protected synchronized void applyEnabledState(boolean enabled) {
        if (enabled) {

            /* Load cached configuration in case APIs are called early. */
            loadConfigurationFromCache();

            /* Load the last stored token and cache it into token context. */
            mTokenStorage.cacheToken();

            /* Download the latest configuration in background. */
            downloadConfiguration();
        } else {
            if (mGetConfigCall != null) {
                mGetConfigCall.cancel();
                mGetConfigCall = null;
            }
            mAuthenticationClient = null;
            mIdentityScope = null;
            completeSignIn(null, new IllegalStateException("Identity is disabled."));
            clearCache();
            removeTokenAndAccount();
        }
    }

    @Override
    protected String getGroupName() {
        return IDENTITY_GROUP;
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    protected String getLoggerTag() {
        return LOG_TAG;
    }

    @Override
    public synchronized void onActivityResumed(Activity activity) {
        mActivity = activity;
        if (mSignInDelayed) {
            signInFromUI();
        }
    }

    @Override
    public synchronized void onActivityPaused(Activity activity) {
        mActivity = null;
    }

    private synchronized void removeTokenAndAccount() {
        mSignInDelayed = false;
        removeAccount(mTokenStorage.getHomeAccountId());
        mTokenStorage.removeToken();
    }

    private synchronized void downloadConfiguration() {

        /* Configure http call to download the configuration. Add ETag if we have a cached entry. */
        HttpClient httpClient = createHttpClient(mContext);
        Map<String, String> headers = new HashMap<>();
        String eTag = SharedPreferencesManager.getString(PREFERENCE_E_TAG_KEY);
        if (eTag != null) {
            headers.put(HEADER_IF_NONE_MATCH, eTag);
        }
        String url = String.format(CONFIG_URL, mAppSecret);
        mGetConfigCall = httpClient.callAsync(url, METHOD_GET, headers, new HttpClient.CallTemplate() {

            @Override
            public String buildRequestBody() {
                return null;
            }

            @Override
            public void onBeforeCalling(URL url, Map<String, String> headers) {
                if (AppCenter.getLogLevel() <= VERBOSE) {
                    String urlString = url.toString().replaceAll(mAppSecret, HttpUtils.hideSecret(mAppSecret));
                    AppCenterLog.verbose(LOG_TAG, "Calling " + urlString + "...");
                    AppCenterLog.verbose(LOG_TAG, "Headers: " + headers);
                }
            }
        }, new ServiceCallback() {

            @Override
            public void onCallSucceeded(final String payload, final Map<String, String> headers) {
                post(new Runnable() {

                    @Override
                    public void run() {
                        processDownloadedConfig(payload, headers.get(HEADER_E_TAG));
                    }
                });
            }

            @Override
            public void onCallFailed(final Exception e) {
                post(new Runnable() {

                    @Override
                    public void run() {
                        if (e instanceof HttpException && ((HttpException) e).getStatusCode() == 304) {
                            processDownloadNotModified();
                        } else {
                            processDownloadError(e);
                        }
                    }
                });
            }
        });
    }

    @WorkerThread
    private synchronized void processDownloadedConfig(String payload, String eTag) {
        mGetConfigCall = null;
        saveConfigFile(payload, eTag);
        AppCenterLog.info(LOG_TAG, "Configure identity from downloaded configuration.");
        boolean configurationValid = initAuthenticationClient(payload);
        if (configurationValid && mSignInDelayed) {
            HandlerUtils.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    signInFromUI();
                }
            });
        }
    }

    private synchronized void processDownloadNotModified() {
        mGetConfigCall = null;
        AppCenterLog.info(LOG_TAG, "Identity configuration didn't change.");
    }

    @WorkerThread
    private void loadConfigurationFromCache() {
        File configFile = getConfigFile();
        if (configFile.exists()) {
            AppCenterLog.info(LOG_TAG, "Configure identity from cached configuration.");
            initAuthenticationClient(FileManager.read(configFile));
        }
    }

    private synchronized void processDownloadError(Exception e) {
        mGetConfigCall = null;
        AppCenterLog.error(LOG_TAG, "Cannot load identity configuration from the server.", e);
    }

    @WorkerThread
    private synchronized boolean initAuthenticationClient(String configurationPayload) {

        /* Parse configuration. */
        try {
            JSONObject configuration = new JSONObject(configurationPayload);
            String identityScope = configuration.getString(IDENTITY_SCOPE);
            String authorityUrl = null;
            JSONArray authorities = configuration.getJSONArray(AUTHORITIES);
            for (int i = 0; i < authorities.length(); i++) {
                JSONObject authority = authorities.getJSONObject(i);
                if (authority.optBoolean(AUTHORITY_DEFAULT) && AUTHORITY_TYPE_B2C.equals(authority.getString(AUTHORITY_TYPE))) {
                    authorityUrl = authority.getString(AUTHORITY_URL);
                    break;
                }
            }
            if (authorityUrl != null) {

                /* The remaining validation is done by the library. */
                mAuthenticationClient = new PublicClientApplication(mContext, getConfigFile());
                mIdentityScope = identityScope;
                AppCenterLog.info(LOG_TAG, "Identity service configured successfully.");
                return true;
            } else {
                throw new IllegalStateException("Cannot find a b2c authority configured to be the default.");
            }
        } catch (JSONException | RuntimeException e) {
            AppCenterLog.error(LOG_TAG, "The configuration is invalid.", e);
            clearCache();
            return false;
        }
    }

    @NonNull
    private File getConfigFile() {
        return new File(mContext.getFilesDir(), FILE_PATH);
    }

    @WorkerThread
    private void saveConfigFile(String payload, String eTag) {
        File file = getConfigFile();
        FileManager.mkdir(file.getParent());
        try {
            FileManager.write(file, payload);
            SharedPreferencesManager.putString(PREFERENCE_E_TAG_KEY, eTag);
            AppCenterLog.debug(LOG_TAG, "Identity configuration saved in cache.");
        } catch (IOException e) {
            AppCenterLog.warn(LOG_TAG, "Failed to cache identity configuration.", e);
        }
    }

    @WorkerThread
    private void clearCache() {
        SharedPreferencesManager.remove(PREFERENCE_E_TAG_KEY);
        FileManager.delete(getConfigFile());
        AppCenterLog.debug(LOG_TAG, "Identity configuration cache cleared.");
    }

    private synchronized AppCenterFuture<SignInResult> instanceSignIn() {
        final DefaultAppCenterFuture<SignInResult> future = new DefaultAppCenterFuture<>();
        if (mPendingSignInFuture != null) {
            future.complete(new SignInResult(null, new IllegalStateException("signIn already in progress.")));
            return future;
        }
        mPendingSignInFuture = future;
        Runnable disabledRunnable = new Runnable() {

            @Override
            public void run() {
                completeSignIn(null, new IllegalStateException("Identity is disabled."));
            }
        };
        post(new Runnable() {

            @Override
            public void run() {
                HandlerUtils.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        signInFromUI();
                    }
                });
            }
        }, disabledRunnable, disabledRunnable);
        return future;
    }

    private void instanceSignOut() {
        post(new Runnable() {

            @Override
            public void run() {
                if (mTokenStorage.getToken() == null) {
                    AppCenterLog.warn(LOG_TAG, "Cannot sign out because a user has not signed in.");
                    return;
                }
                removeTokenAndAccount();
                AppCenterLog.info(LOG_TAG, "User sign-out succeeded.");
            }
        });
    }

    private void removeAccount(final String homeAccountIdentifier) {
        if (mAuthenticationClient == null || homeAccountIdentifier == null) {
            return;
        }
        mAuthenticationClient.getAccounts(new PublicClientApplication.AccountsLoadedListener() {

            @Override
            public void onAccountsLoaded(final List<IAccount> accounts) {
                post(new Runnable() {

                    @Override
                    public void run() {
                        for (IAccount account : accounts) {
                            if (account.getHomeAccountIdentifier().getIdentifier().equals(homeAccountIdentifier)) {
                                mAuthenticationClient.removeAccount(account);
                            }
                        }
                    }
                });
            }
        });
    }

    @UiThread
    private synchronized void signInFromUI() {
        if (mAuthenticationClient != null && mActivity != null) {
            AppCenterLog.info(LOG_TAG, "Signing in using browser.");
            mSignInDelayed = false;
            mAuthenticationClient.acquireToken(mActivity, new String[]{mIdentityScope}, new AuthenticationCallback() {

                @Override
                public void onSuccess(final IAuthenticationResult authenticationResult) {
                    post(new Runnable() {

                        @Override
                        public void run() {
                            IAccount account = authenticationResult.getAccount();
                            String homeAccountId = account.getHomeAccountIdentifier().getIdentifier();
                            mTokenStorage.saveToken(authenticationResult.getIdToken(), homeAccountId);
                            String accountId = account.getAccountIdentifier().getIdentifier();
                            AppCenterLog.info(LOG_TAG, "User sign-in succeeded.");
                            completeSignIn(new UserInformation(accountId), null);
                        }
                    });
                }

                @Override
                public void onError(MsalException exception) {
                    AppCenterLog.error(LOG_TAG, "User sign-in failed.", exception);
                    completeSignIn(null, exception);
                }

                @Override
                public void onCancel() {
                    AppCenterLog.warn(LOG_TAG, "User canceled sign-in.");
                    completeSignIn(null, new CancellationException("User cancelled sign-in."));
                }
            });
        } else {
            AppCenterLog.debug(LOG_TAG, "signIn is called while it's not configured or not in the foreground, waiting.");
            mSignInDelayed = true;
        }
    }

    private synchronized void completeSignIn(UserInformation userInformation, Exception exception) {
        if (mPendingSignInFuture != null) {
            mPendingSignInFuture.complete(new SignInResult(userInformation, exception));
            mPendingSignInFuture = null;
        }
    }

    @VisibleForTesting
    boolean isSignInDelayed() {
        return mSignInDelayed;
    }
}
