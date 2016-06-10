package com.mopub.mobileads;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.mopub.common.UrlAction;
import com.mopub.common.UrlHandler;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Intents;
import com.mopub.exceptions.IntentNotResolvableException;

import java.util.EnumSet;

import static com.mopub.mobileads.MoPubErrorCode.UNSPECIFIED;

class HtmlWebViewClient extends WebViewClient {
    static final String MOPUB_FINISH_LOAD = "mopub://finishLoad";
    static final String MOPUB_FAIL_LOAD = "mopub://failLoad";

    private final EnumSet<UrlAction> SUPPORTED_URL_ACTIONS = EnumSet.of(
            UrlAction.HANDLE_MOPUB_SCHEME,
            UrlAction.IGNORE_ABOUT_SCHEME,
            UrlAction.HANDLE_PHONE_SCHEME,
            UrlAction.OPEN_APP_MARKET,
            UrlAction.OPEN_NATIVE_BROWSER,
            UrlAction.OPEN_IN_APP_BROWSER,
            UrlAction.HANDLE_SHARE_TWEET,
            UrlAction.FOLLOW_DEEP_LINK_WITH_FALLBACK,
            UrlAction.FOLLOW_DEEP_LINK);

    private final Context mContext;
    private final String mDspCreativeId;
    private HtmlWebViewListener mHtmlWebViewListener;
    private BaseHtmlWebView mHtmlWebView;
    private final String mClickthroughUrl;
    private final String mRedirectUrl;

    HtmlWebViewClient(HtmlWebViewListener htmlWebViewListener,
                      BaseHtmlWebView htmlWebView, String clickthrough,
                      String redirect, String dspCreativeId) {
        mHtmlWebViewListener = htmlWebViewListener;
        mHtmlWebView = htmlWebView;
        mClickthroughUrl = clickthrough;
        mRedirectUrl = redirect;
        mDspCreativeId = dspCreativeId;
        mContext = htmlWebView.getContext();
    }

    @Override
    public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
        new UrlHandler.Builder()
                .withDspCreativeId(mDspCreativeId)
                .withSupportedUrlActions(SUPPORTED_URL_ACTIONS)
                .withResultActions(new UrlHandler.ResultActions() {
                    @Override
                    public void urlHandlingSucceeded(@NonNull String url,
                                                     @NonNull UrlAction urlAction) {
                        if (mHtmlWebView.wasClicked()) {
                            mHtmlWebViewListener.onClicked();
                            mHtmlWebView.onResetUserClick();
                        }
                    }

                    @Override
                    public void urlHandlingFailed(@NonNull String url,
                                                  @NonNull UrlAction lastFailedUrlAction) {
                    }
                })
                .withMoPubSchemeListener(new UrlHandler.MoPubSchemeListener() {
                    @Override
                    public void onFinishLoad() {
                        mHtmlWebViewListener.onLoaded(mHtmlWebView);
                    }

                    @Override
                    public void onClose() {
                        mHtmlWebViewListener.onCollapsed();
                    }

                    @Override
                    public void onFailLoad() {
                        mHtmlWebViewListener.onFailed(UNSPECIFIED);
                    }
                })
                .build().handleUrl(mContext, url, mHtmlWebView.wasClicked());
        return true;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        // If the URL being loaded shares the redirectUrl prefix, open it in the browser.
        if (mRedirectUrl != null && url.startsWith(mRedirectUrl)) {
            view.stopLoading();
            if (mHtmlWebView.wasClicked()) {
                try {
                    Intents.showMoPubBrowserForUrl(mContext, Uri.parse(url), mDspCreativeId);
                } catch (IntentNotResolvableException e) {
                    MoPubLog.d(e.getMessage());
                }
            } else {
                MoPubLog.d("Attempted to redirect without user interaction");
            }
        }
    }

    @Override
    public void onPageFinished(final WebView view, String url) {
        view.setBackgroundColor(Color.parseColor("#212121"));
        /*
        We intercept the html response here and use javascript to traverse the dom tree until
        a img tag is found.
        If only one image is found and the image size is greater than 100,000 pixels, we clear the height / width attributes & calculate
        the image aspect ratio and the devices aspect ratio. We then resize the image so it fits inside the device screen while maintaining
        its aspect ratio.
        The images are usually 320 x 480 (3:2) and don't fill the whole screen (devices are typically 16:9).
         */

//        view.loadUrl("javascript:function setImageSize(){document.body.style.margin=\"0px\";var e=document.getElementsByTagName(\"img\");if(e){var t=e[0];var n=t.width*t.height;if(n>1e5){var r=t.width/t.height;var i=window.innerWidth/window.innerHeight;var s;var o;if(i==r){s=window.innerHeight;o=window.innerWidth}else if(i>r){s=window.innerHeight;o=t.width*(window.innerHeight/t.height)}else{s=t.height*(window.innerWidth/t.width);o=window.innerWidth}t.style.height=s+\"px\";t.style.width=o+\"px\"}}}setImageSize();");
    }
}
