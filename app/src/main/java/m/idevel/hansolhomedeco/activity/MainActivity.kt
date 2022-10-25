package m.idevel.hansolhomedeco.activity


import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.MediaScannerConnection
import android.net.ConnectivityManager
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.Window
import android.view.WindowManager
import android.webkit.*
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import api.OnResultListener
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.onestore.iap.api.PurchaseClient.SecurityException
import kr.co.medialog.ApiManager
import kr.co.medialog.SettingInfoData
import m.idevel.hansolhomedeco.R
import m.idevel.hansolhomedeco.broadcast.DataSaverChangeReceiver
import m.idevel.hansolhomedeco.broadcast.NetworkChangeReceiver
import m.idevel.hansolhomedeco.dialog.AgentPopupDialog
import m.idevel.hansolhomedeco.dialog.CustomAlertDialog
import m.idevel.hansolhomedeco.fcm.PushDataNotiParser
import m.idevel.hansolhomedeco.fcm.PushPreferences.IS_NOTI
import m.idevel.hansolhomedeco.fcm.PushPreferences.PUSH_DATA_LINK_TYPE
import m.idevel.hansolhomedeco.fcm.PushPreferences.PUSH_DATA_LINK_URL
import m.idevel.hansolhomedeco.fcm.PushPreferences.PUSH_DATA_SHOWTIME
import m.idevel.hansolhomedeco.interfaces.IDataSaverListener
import m.idevel.hansolhomedeco.interfaces.NetworkChangeListener
import m.idevel.hansolhomedeco.utils.*
import m.idevel.hansolhomedeco.web.BaseWebView
import m.idevel.hansolhomedeco.web.MyWebChromeClient
import m.idevel.hansolhomedeco.web.UrlData
import m.idevel.hansolhomedeco.web.constdata.*
import m.idevel.hansolhomedeco.web.interfaces.IWebBridgeApi
import okhttp3.ResponseBody
import java.io.*
import java.lang.ref.WeakReference
import java.net.URISyntaxException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess


/**
 * main activity class.
 *
 * @author : jjbae
 */
class MainActivity : FragmentActivity() {
    private var mAgentPopupDialog: AgentPopupDialog? = null

    private var mImageSpeechVoice: ImageView? = null
    private var mtextSpeechMessage: TextView? = null
    private var mSpeechLayout: RelativeLayout? = null
    private var mSpeechBtnLayout: RelativeLayout? = null
    private var mBtnSpeechRetry: Button? = null
    private var mBtnSpeechCancel: Button? = null

    private var mSplashView: View? = null //초기 로딩시 보여주기 위한 view.
    private var mErrorView: View? = null //The network error view.
    private var mWebview: BaseWebView? = null //mobile view page 연결을 위한 webview.

    private var mPermissionWebview: BaseWebView? = null //mobile view page 연결을 위한 webview.

    private var mWebViewClient: m.idevel.hansolhomedeco.web.MyWebViewClient? = null //The web view client.
    private var mWebChromeClient: MyWebChromeClient? = null //The web chrome client.
    private var isRestartApp = false //The is restart app.

    private var mWebviewSub: RelativeLayout? = null // sub webView parent
    private var mSettingdata: SettingInfoData? = null

    private val mHandler = WeakHandler(this) //UI handler

    private var mReTry: Int = 0
    private var mLocationManager: LocationManager? = null

    private var mApiManager: ApiManager? = null
    private val mNetworkChangeReceiver = NetworkChangeReceiver() //네트워크 check
    private val mDataSaverChangeReceiver = DataSaverChangeReceiver()
    var isIntoNotiLandingUrl: Boolean = false

    private var test_btn1: Button? = null
    private var test_btn2: Button? = null

    private var photoURI: Uri? = null
    private var mFilePathCallback: ValueCallback<Array<Uri?>?>? = null

    private var mAlertDialog: CustomAlertDialog? = null

    private var recordDownloadTask: AsyncTask<Void, Void, Boolean>? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main)

        mApiManager = ApiManager.getInstance(this@MainActivity)
        mWebviewSub = findViewById(R.id.webview_sub)
        mWebview = findViewById(R.id.webview_main)
        mPermissionWebview = findViewById(R.id.permission_web)

        mImageSpeechVoice = findViewById(R.id.image_speech_voice)
        mtextSpeechMessage = findViewById(R.id.text_speech_message)
        mSpeechLayout = findViewById(R.id.speech_layout)

        mSpeechBtnLayout = findViewById(R.id.speech_btn_layout)
        mBtnSpeechRetry = findViewById(R.id.btn_speech_retry)
        mBtnSpeechCancel = findViewById(R.id.btn_speech_cancel)

        mBtnSpeechCancel?.setOnClickListener {
        }

        mBtnSpeechRetry?.setOnClickListener {
        }

        mSplashView = findViewById(R.id.view_splash)
        mSplashView?.visibility = View.VISIBLE
        mErrorView = findViewById(R.id.view_error)
        mErrorView?.visibility = View.GONE

        cleanCookie()

        //네트워크 연결 여부
        if (!isNetworkConnected(this)) {
            showErrorDlg(NETWORK_CONNECTION_ERROR)
            return
        }

        // 앱 사용 중 data saver 사용 Listener
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mDataSaverChangeReceiver.setListener(dataSaverListener)
        }

        // 앱 사용 중 data network 변경 Listener
        mNetworkChangeReceiver.setListener(networkListener)

        val isPermissionNoti = SharedPreferencesUtil.getBoolean(this@MainActivity, SharedPreferencesUtil.Cmd.PERMISSION_NOTI)
        if (!isPermissionNoti) {
            mHandler.sendEmptyMessageDelayed(HANDLER_PERMISSIONS, 1000L)
        } else {
            checkSettingInfo()
        }

        checkSchemeIntent(intent)
    }

    // push 클릭
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        checkSchemeIntent(intent)
    }

    override fun onResume() {
        super.onResume()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            registerReceiver(mDataSaverChangeReceiver, IntentFilter(ConnectivityManager.ACTION_RESTRICT_BACKGROUND_CHANGED))
        }

        registerReceiver(mNetworkChangeReceiver, IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"))

        mWebview?.sendEvent(IdevelServerScript.SET_APP_STATUS, AppStatusInfo("onResume").toJsonString())
        mWebview?.onResume()

        mPermissionWebview?.onResume()
    }

    override fun onPause() {
        super.onPause()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            unregisterReceiver(mDataSaverChangeReceiver)
        }

        unregisterReceiver(mNetworkChangeReceiver)

        mWebview?.sendEvent(IdevelServerScript.SET_APP_STATUS, AppStatusInfo("onPause").toJsonString())
        mWebview?.onPause()

        mPermissionWebview?.onPause()
    }

    private fun checkSettingInfo() {
        mHandler.sendEmptyMessageDelayed(HANDLER_SPLASH, 1000L)
    }

    /**
     * 스플래시 애니메이션 설정
     */
    private fun setSplash() {
        mHandler.sendEmptyMessageDelayed(HANDLER_SPLASH_DELAY, 300L)
    }

    /**
     * Sets the main view.
     */
    private fun setMainView() {
        if (mWebview == null) {
            return
        }

        mHandler.sendEmptyMessageDelayed(HANDLER_NETWORK_TIMER, PING_TIME.toLong())

        mWebview?.setBackgroundColor(Color.WHITE)
        mWebview?.setJSInterface(iWebBridgeApi)
        mWebview?.loadUrl(UrlData.getMainUrl(this@MainActivity)!!)

        mWebViewClient = object : m.idevel.hansolhomedeco.web.MyWebViewClient(this) {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)

                DLog.e("bjj mWebViewClient onPageStarted : $url")
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)

                if (url.startsWith(UrlData.getMainUrl(this@MainActivity)!!)) {
                    mWebview?.bringToFront()
                    removeSplash()
                } else {
                    //TODO
                    mWebview?.bringToFront()
                    removeSplash()
                }

                DLog.e("bjj mWebViewClient onPageFinished : " +
                        "$url, " +
                        "${mSettingdata?.main_url} ^ "
                        + UrlData.getMainUrl(this@MainActivity) + " ^ "
                        + url.startsWith(UrlData.getMainUrl(this@MainActivity)!!))
            }

            override fun showErrorPage() {
                DLog.e("bjj mWebViewClient showErrorPage : ")

                showErrorView()
            }

            override fun setUntouchableProgress(visible: Int) {
                DLog.e("bjj mWebViewClient setUntouchableProgress : $visible")
            }

            @SuppressWarnings("deprecation")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                DLog.e("bjj mWebViewClient shouldOverrideUrlLoading deprecation : $url")

                return urlLoading(view, Uri.parse(url))
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    DLog.e("bjj mWebViewClient shouldOverrideUrlLoading : ${request?.getUrl()}")

                    urlLoading(view, request?.getUrl())
                } else {
                    super.shouldOverrideUrlLoading(view, request)
                }
            }
        }

        mWebChromeClient = object : MyWebChromeClient(this, findViewById<View>(R.id.mainview) as RelativeLayout) {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                if (View.GONE == mSplashView?.visibility) {
                    super.onProgressChanged(view, newProgress)
                }
            }

            override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
                return super.onJsAlert(view, url, message, result)
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                return super.onConsoleMessage(consoleMessage)
            }

            override fun onShowFileChooser(webView: WebView?, filePathCallback: ValueCallback<Array<Uri?>?>, fileChooserParams: FileChooserParams?): Boolean {
                val isCapture = fileChooserParams!!.isCaptureEnabled
                val acceptType = fileChooserParams.acceptTypes[0].toString() //access 타입을 체크합니다.

                DLog.e("bjj mWebViewClient onShowFileChooser : "
                        + isCapture + " ^ "
                        + acceptType + " ^ "
                        + fileChooserParams.title + " ^ "
                        + fileChooserParams.filenameHint + " ^ "
                        + fileChooserParams.mode + " ^ "
                        + webView?.url)

                if (mFilePathCallback != null) {
                    mFilePathCallback?.onReceiveValue(null)
                    mFilePathCallback = null
                }

                mFilePathCallback = filePathCallback

                runCamera(false)

                return true
            }
        }

        mWebview?.webViewClient = mWebViewClient!!
        mWebview?.webChromeClient = mWebChromeClient!!
    }

    // 카메라 기능 구현
    private fun runCamera(isCapture: Boolean) {
        val intentCamera = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "m.idevel.hansolhomedeco_capture_$timeStamp.jpg"

        createImageUri(imageFileName, "image/jpg")?.let { uri ->
            photoURI = uri
        }

        intentCamera.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)

        if (!isCapture) { // 선택팝업 카메라, 갤러리 둘다 띄우고 싶을 때
            val pickIntent = Intent(Intent.ACTION_PICK)
            pickIntent.type = MediaStore.Images.Media.CONTENT_TYPE
            pickIntent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

            val pickTitle = "사진 가져올 방법을 선택하세요."
            val chooserIntent = Intent.createChooser(pickIntent, pickTitle)

            // 카메라 intent 포함시키기..
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf<Parcelable>(intentCamera))
            startActivityForResult(chooserIntent, PICK_FROM_ALBUM_NEW)
        } else { // 바로 카메라 실행..
            startActivityForResult(intentCamera, PICK_FROM_ALBUM_NEW)
        }
    }

    fun createImageUri(filename: String, mimeType: String): Uri? {
        var values = ContentValues()
        values.put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        values.put(MediaStore.Images.Media.MIME_TYPE, mimeType)
        return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }

    private fun checkPushData(intent: Intent) {
        // 푸시를 통해 진입한 경우가 아님
        DLog.e("bjj checkPushData :: " + intent.extras)

        if (null == intent.extras) {
            return
        }

        if (mAgentPopupDialog != null) {
            mAgentPopupDialog!!.dismiss()
            mAgentPopupDialog = null
        }

        // 8.0미만 버전에서 뱃지 카운트 0 으로 설정
        setIconBadge(this, 0)

        val linkUrl = intent.getStringExtra(PUSH_DATA_LINK_URL)
        mAgentPopupDialog = AgentPopupDialog(this, intent)

        mAgentPopupDialog!!.setOkClickListener(OnClickListener {
            mAgentPopupDialog!!.dismiss()

            if (linkUrl == null || linkUrl.equals("", ignoreCase = true)) {
                return@OnClickListener
            } else {
                if (mWebview != null) {
                    mWebview!!.loadUrl(linkUrl)
                }
            }
        })

        mAgentPopupDialog!!.show()

        val showTime = intent.getStringExtra(PUSH_DATA_SHOWTIME)

        if (!showTime.isNullOrEmpty()) {
            Handler().postDelayed({
                if (mAgentPopupDialog!!.isShowing) {
                    mAgentPopupDialog!!.setDisappearClose()
                }
            }, showTime.toLong())
        }
    }

    /**
     * Show error view.
     */
    fun showErrorView() {
        mErrorView?.visibility = View.VISIBLE

        val homeBtn = mErrorView?.findViewById<Button>(R.id.homeBtn)
        homeBtn?.setOnClickListener { finish() }
    }

    /**
     * Show PermissionNoti view.
     */
    private fun showPermissionNoti() {
        if (mPermissionWebview == null) {
            return
        }

        mPermissionWebview?.setBackgroundColor(Color.WHITE)
        mPermissionWebview?.loadUrl(UrlData.PERMISSIONS_URL)

        val PermissionWebViewClient = object : m.idevel.hansolhomedeco.web.MyWebViewClient(this) {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)

                DLog.e("bjj showPermissionNoti mWebViewClient onPageStarted : $url")

                if (url == "https://www.hansolhomedecolasola.com/app/intro.php") {
                    SharedPreferencesUtil.setBoolean(this@MainActivity, SharedPreferencesUtil.Cmd.PERMISSION_NOTI, true)


                    mPermissionWebview?.let {
                        webviewDestroy(it)
                    }
                    mSplashView?.bringToFront()
                    mSplashView?.visibility = View.VISIBLE

                    checkSettingInfo()
                }
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)

                DLog.e("bjj showPermissionNoti mWebViewClient onPageFinished : $url")

                if (url == UrlData.PERMISSIONS_URL) {
                    removeSplash()
                    mPermissionWebview?.bringToFront()
                }
            }

            override fun showErrorPage() {
                DLog.e("bjj showPermissionNoti mWebViewClient showErrorPage : ")

                showErrorView()
            }
        }

        mPermissionWebview?.webViewClient = PermissionWebViewClient
    }

    /**
     * Show main view.
     */
    private fun showMainView() {
        mHandler.removeMessages(HANDLER_NETWORK_TIMER)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        mSplashView?.visibility = View.GONE
        mErrorView?.visibility = View.GONE

        startTestBtn()
    }

    private fun showAppFinishPopup() {
        val alertDialog = CustomAlertDialog(this)
        alertDialog.setCancelable(true)
        alertDialog.setDataSaveLayout(0, R.string.popup_app_finish_message)
        alertDialog.setButtonString(R.string.popup_app_finish_ok, R.string.popup_app_finish_cancel)

        alertDialog.setOkClickListener(OnClickListener { v ->
            when (v.id) {
                R.id.btn_ok -> {
                    alertDialog.dismiss()

                    finish()
                }
                R.id.btn_cancel -> {
                    alertDialog.dismiss()
                }
            }
        })

        if (!isFinishing) {
            alertDialog.show()
        }
    }

    /**
     * Show other app version dlg.
     */
    private fun showOtherAppVersionDlg() {
        val alertDialog = CustomAlertDialog(this)
        alertDialog?.setOkClickListener(OnClickListener { v ->
            alertDialog?.dismiss()
            when (v.id) {
                R.id.btn_ok -> gotoPlayStore()
                R.id.btn_cancel -> finish()
            }
        })

        if (!isFinishing && !isDestroyed) {
            alertDialog?.show()
        }
    }

    private fun showDataSaveDlg(title: Int, content: Int) {
        val alertDialog = CustomAlertDialog(this)
        alertDialog?.setCancelable(false)
        alertDialog?.setDataSaveLayout(title, content)
        alertDialog?.setButtonString(R.string.popup_btn_ok_dta_save, R.string.popup_btn_cancel_dta_save)

        alertDialog?.setOkClickListener(OnClickListener { v ->
            alertDialog!!.dismiss()
            when (v.id) {
                R.id.btn_cancel -> finish()
                R.id.btn_ok -> {
                    val intent = Intent()
                    intent.action = Settings.ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri

                    startActivity(intent)
                    finish()
                }
            }
        })

        if (!isFinishing) {
            alertDialog?.show()
        }
    }

    /**
     * Show alert dlg.
     */
    private fun showAlertDlg(title: Int, content: Int, errorType: Int) {
        if (isDestroyed || isFinishing) {
            return
        }

        if (mAlertDialog != null) {
            if (mAlertDialog!!.isShowing) {
                mAlertDialog!!.dismiss()
            }
        }

        mAlertDialog = CustomAlertDialog(this)
        mAlertDialog!!.setErrorLayout(title, content)

        mAlertDialog!!.setOkClickListener(OnClickListener { v ->
            mAlertDialog!!.dismiss()

            if (v.id == R.id.btn_error) {
                when (errorType) {
                    NETWORK_CONNECTION_ERROR, TIMEOUT_ERROR -> {
                        finish()
                    }
                    APP_VERSION_CHECK -> {
                        if (mReTry > 2) {
                            finish()
                        } else {
                            mReTry++
                            checkSettingInfo()
                        }
                    }
                    else -> {
                    }
                }
            }
        })

        mAlertDialog!!.show()
    }

    /**
     * Show PermissionDenyDialog dlg.
     */
    private fun showPermissionDenyDialog(permissionStr: String) {
        val alertDialog = CustomAlertDialog(this)
        alertDialog.setCancelable(false)

        alertDialog.setDataSaveLayout(R.string.permissionDeny_title, R.string.permissionDeny_msg4)
        alertDialog.setButtonString(R.string.popup_btn_ok_dta_save, R.string.popup_btn_cancel_dta_save)

        alertDialog.setOkClickListener(OnClickListener { v ->
            alertDialog.dismiss()
            when (v.id) {
                R.id.btn_cancel -> finish()
                R.id.btn_ok -> {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)

                    finish()
                }
            }
        })

        if (!isFinishing) {
            alertDialog.show()
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun gotoOverlayPermission() {
        val i = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${this@MainActivity.packageName}"))
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        ContextCompat.startActivity(this@MainActivity, i, ActivityOptionsCompat.makeBasic().toBundle())
    }

    /**
     * Show network error dlg.
     *
     * @param errorType the error type
     */
    private fun showErrorDlg(errorType: Int) {
        var titleRes = R.string.popup_title_server_error
        var msgRes = R.string.popup_msg_server_error

        when (errorType) {
            NETWORK_CONNECTION_ERROR -> {
                titleRes = R.string.popup_title_network_error
                msgRes = R.string.popup_msg_network_error
            }
            TIMEOUT_ERROR -> {
                titleRes = R.string.popup_title_server_error
                msgRes = R.string.popup_msg_server_error
            }
        }

        showAlertDlg(titleRes, msgRes, errorType)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (KeyEvent.KEYCODE_BACK == keyCode) {
            DLog.e("bjj onKeyDown ==>> " + mWebChromeClient?.childLayout?.visibility + " ^ " + mWebview!!.canGoBack())

            if (mWebChromeClient?.childLayout?.visibility == View.VISIBLE) {
                mWebChromeClient?.childLayout?.visibility = View.GONE

                mWebview!!.loadUrl("javascript:window.close();")

                return false
            }

            mWebview?.let {
                if (it.canGoBack()) {
                    it.goBack()
                } else {
                    showAppFinishPopup()
                }

                return false
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    private fun webviewDestroy(webview: BaseWebView) {
        webview?.let {
            it.stopLoading()
            it.removeAllViews()
            it.clearHistory()
            it.clearCache(true)
            it.destroy()
        }
    }

    /**
     * Goto play store.
     */
    private fun gotoPlayStore() {
        val intent = Intent(Intent.ACTION_VIEW)

        intent.data = Uri.parse("market://details?id=$packageName")

        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        mWebview?.sendEvent(IdevelServerScript.SET_APP_STATUS, AppStatusInfo("onDestroy").toJsonString())

        cleanCookie()

        mPermissionWebview?.let {
            webviewDestroy(it)
        }

        mWebview?.let {
            webviewDestroy(it)
        }

        super.onDestroy()
    }

    /**
     * Restart app.
     */
    fun restartApp() {
        cleanCookie()
        isRestartApp = true

        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val componentName = intent!!.component
        val mainIntent = Intent.makeRestartActivityTask(componentName)

        startActivity(mainIntent)

        exitProcess(0)
    }

    /**
     * Clean cookie.
     */
    private fun cleanCookie() {
        DLog.e("isRestartApp ==>> $isRestartApp")

        if (!isRestartApp) {
            this@MainActivity.runOnUiThread {
                mWebview?.clearCache(true)
                mWebview?.clearHistory()

                // web 요청으로 주석 처리
//                val cookieSyncMngr = CookieSyncManager.createInstance(this@MainActivity)
//                cookieSyncMngr.startSync()
//                val cookieManager = CookieManager.getInstance()
//                cookieManager.removeAllCookie()
//                cookieManager.removeSessionCookie()
//                cookieSyncMngr.stopSync()
            }
        }
    }

    public override fun onStart() {
        super.onStart()
    }

    public override fun onStop() {
        super.onStop()
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        when (requestCode) {
            PICK_FROM_ALBUM_NEW -> {
                if (resultCode == Activity.RESULT_OK) {
                    var myIntent = intent

                    if (mFilePathCallback == null) {
                        return
                    }

                    if (myIntent == null) {
                        myIntent = Intent()
                    }

                    if (myIntent.getData() == null) {
                        myIntent.setData(photoURI)
                    }

                    mFilePathCallback?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, myIntent))
                    mFilePathCallback = null
                } else {
                    if (mFilePathCallback != null) {   //  resultCode에 RESULT_OK가 들어오지 않으면 null 처리하지 한다.(이렇게 하지 않으면 다음부터 input 태그를 클릭해도 반응하지 않음)
                        mFilePathCallback?.onReceiveValue(null)
                        mFilePathCallback = null
                    }
                }
            }

            PICK_FROM_ALBUM -> {
            }

            PICK_FROM_CAMERA -> {
            }

            DEV_REQUEST_CODE -> {
                checkSettingInfo()
            }

            TEL_REQUEST_CODE -> {
            }

            WEB_INTENT_REQUEST_CODE -> {
            }

            REQUEST_QRSCAN_ACTIVITY -> { //QR
                if (resultCode == Activity.RESULT_OK) {
                    val qrResult = intent?.getStringExtra(QrcodeScanActivity.CALL_BACK)

                    DLog.e("bjj REQUEST_QRSCAN_ACTIVITY :: " + qrResult)

                    if (qrResult.isNullOrEmpty()) {
                        mWebview?.sendEvent(IdevelServerScript.OPEN_QR, QrInfo("").toJsonString())
                    } else {
                        mWebview?.sendEvent(IdevelServerScript.OPEN_QR, QrInfo(qrResult).toJsonString())
                    }
                } else {
                    val qrResult = intent?.getStringExtra(QrcodeScanActivity.RESTART)

                    DLog.e("bjj QrcodeScanActivity RESULT_CANCELED " + qrResult)

                    if (qrResult.isNullOrEmpty()) {
                        mWebview?.sendEvent(IdevelServerScript.OPEN_QR, QrInfo("").toJsonString())
                    } else {
                        Handler().postDelayed({
                            val intent = Intent(this@MainActivity, QrcodeScanActivity::class.java)

                            if (qrResult == "FLASH_ON") {
                                intent.putExtra(QrcodeScanActivity.IS_FLASH, true)
                            } else {
                                intent.putExtra(QrcodeScanActivity.IS_FLASH, false)
                            }

                            startActivityForResult(intent, REQUEST_QRSCAN_ACTIVITY)
                        }, 0L)
                    }
                }
            }
        }
    }

    /**
     * 권한 체크
     */
    private fun checkPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Dexter.withContext(this).withPermissions(m.idevel.hansolhomedeco.MyApplication.PERMISSIONS).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    report.let {
                        DLog.e("bjj PERMISSION checkPermission onPermissionsChecked aa ==>> "
                                + report.areAllPermissionsGranted() + " ^ "
                                + it.areAllPermissionsGranted() + " ^ "
                                + it.isAnyPermissionPermanentlyDenied)

                        if (report.areAllPermissionsGranted()) { // 모든 권한 허용
                            setMainView()
                        } else if (report.isAnyPermissionPermanentlyDenied) {
                            DLog.e("bjj PERMISSION checkPermission onPermissionsChecked bb ==>> " + Build.VERSION.SDK_INT)

                            showPermissionDenyDialog("")
                        } else {
                            finish()
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(permissions: MutableList<PermissionRequest>?, token: PermissionToken?) {
                    DLog.e("bjj PERMISSION checkPermission onPermissionRationaleShouldBeShown ==>> " + permissions)

                    token?.continuePermissionRequest()
                }
            }).onSameThread().check()

            return false
        }

        return true
    }

    private fun removeSplash() {
        if (isFinishing) {
            return
        }

        (this@MainActivity as Activity).runOnUiThread {
            setSplash()
        }
    }

    //공유 팝업노출
    private fun openSharePopup(url: String) {
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_TEXT, url)
        sendIntent.type = "text/plain"

        startActivity(Intent.createChooser(sendIntent, ""))
    }

    // web page clear history
    private fun pageClearHistory() {
        mWebview?.clearHistory()
        mWebview?.removeAllViews()
    }

    /**
     * WEB INTERFACE
     */
    private val iWebBridgeApi = object : IWebBridgeApi {
        override fun pageClearHistory() {
            (this@MainActivity as Activity).runOnUiThread {
                this@MainActivity.pageClearHistory()
            }
        }

        override fun openSharePopup(url: String) {
            (this@MainActivity as Activity).runOnUiThread {
                this@MainActivity.openSharePopup(url)
            }
        }

        override fun getPushRegId() {
            (this@MainActivity as Activity).runOnUiThread {
                val regId = SharedPreferencesUtil.getString(this@MainActivity, SharedPreferencesUtil.Cmd.PUSH_REG_ID)
                mWebview?.sendEvent(IdevelServerScript.GET_PUSH_REG_ID, getPushRegIdInfo(regId!!, "AOS").toJsonString())
            }
        }

        override fun restartApp() {
            (this@MainActivity as Activity).runOnUiThread {
                this@MainActivity.restartApp()
            }
        }

        override fun finishApp() {
            (this@MainActivity as Activity).runOnUiThread {
                System.exit(0)
            }
        }

        override fun getAppVersion() {
            (this@MainActivity as Activity).runOnUiThread {
                val version = getVersionName(this@MainActivity)
                mWebview?.sendEvent(IdevelServerScript.GET_APP_VERSION, GetAppVersionInfo(version!!).toJsonString())
            }
        }

        override fun requestCallPhone(data: RequestCallPhoneInfo) {
            (this@MainActivity as Activity).runOnUiThread {
                try {
                    val callIntent = Intent(Intent.ACTION_DIAL)
                    callIntent.data = Uri.parse("tel:${data.phoneNumber}")
                    startActivity(callIntent)
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }
            }
        }

        override fun requestExternalWeb(data: RequestExternalWebInfo) {
            (this@MainActivity as Activity).runOnUiThread {
                intent = Intent(Intent.ACTION_VIEW, Uri.parse(data.url))
                startActivity(intent)
            }
        }

        override fun removeSplash() {
            (this@MainActivity as Activity).runOnUiThread {
                this@MainActivity.removeSplash()
            }
        }

        override fun getGpsInfo() {
            (this@MainActivity as Activity).runOnUiThread {
                this@MainActivity.getGPSLoacation()
            }
        }

        override fun readyOneStoreBilling() {
//            (this@MainActivity as Activity).runOnUiThread {
//                this@MainActivity.initOneStore()
//            }
        }

        override fun requestBuyProduct(data: RequestBuyProductInfo) {
//            (this@MainActivity as Activity).runOnUiThread {
//                val productType = getItemType(data.productId)
//                this@MainActivity.requestBilling(data.productId, productType)
//            }
        }

        override fun openCamera(type: String, param: String) {
            (this@MainActivity as Activity).runOnUiThread {
            }
        }

        override fun openGallery(type: String, param: String) {
            (this@MainActivity as Activity).runOnUiThread {
            }
        }

        override fun setPushVibrate(isBool: Boolean) {
            (this@MainActivity as Activity).runOnUiThread {
                SharedPreferencesUtil.setBoolean(this@MainActivity, SharedPreferencesUtil.Cmd.PUSH_VIBRATE, isBool)
            }
        }

        override fun setPushBeep(isBool: Boolean) {
            (this@MainActivity as Activity).runOnUiThread {
                SharedPreferencesUtil.setBoolean(this@MainActivity, SharedPreferencesUtil.Cmd.PUSH_BEEP, isBool)
            }
        }

        override fun setAutoLogin(isAuto: Boolean, token: String) {
            (this@MainActivity as Activity).runOnUiThread {
                SharedPreferencesUtil.setBoolean(this@MainActivity, SharedPreferencesUtil.Cmd.AUTO_LOGIN, isAuto)
                SharedPreferencesUtil.setString(this@MainActivity, SharedPreferencesUtil.Cmd.TOKEN, token)
            }
        }

        override fun getAutoLogin() {
            (this@MainActivity as Activity).runOnUiThread {
                val isAuto = SharedPreferencesUtil.getBoolean(this@MainActivity, SharedPreferencesUtil.Cmd.AUTO_LOGIN)
                val token = SharedPreferencesUtil.getString(this@MainActivity, SharedPreferencesUtil.Cmd.TOKEN)

                mWebview?.sendEvent(IdevelServerScript.GET_AUTO_LOGIN, AutoLoginInfo(isAuto, token ?: "null").toJsonString())
            }
        }

        override fun setAccount(id: String, pw: String) {
            (this@MainActivity as Activity).runOnUiThread {
                SharedPreferencesUtil.setString(this@MainActivity, SharedPreferencesUtil.Cmd.AUTO_LOGIN_ID, id)
                SharedPreferencesUtil.setString(this@MainActivity, SharedPreferencesUtil.Cmd.AUTO_LOGIN_PW, pw)
            }
        }

        override fun getAccount() {
            (this@MainActivity as Activity).runOnUiThread {
                val id = SharedPreferencesUtil.getString(this@MainActivity, SharedPreferencesUtil.Cmd.AUTO_LOGIN_ID)
                val pw = SharedPreferencesUtil.getString(this@MainActivity, SharedPreferencesUtil.Cmd.AUTO_LOGIN_PW)

                mWebview?.sendEvent(IdevelServerScript.GET_ACCOUNT, AccountInfo(id!!, pw!!).toJsonString())
            }
        }

        override fun downloadFile(fileURL: String, fileName: String, type: String) {
            (this@MainActivity as Activity).runOnUiThread {
                downloadFileTask(fileURL, fileName, type)
            }
        }

        override fun callBiometrics() {
            DLog.e("bjj callBiometrics :: ")

            (this@MainActivity as Activity).runOnUiThread {
            }
        }

        override fun callStt(locale: String) {
            DLog.e("bjj callStt :: ")

            (this@MainActivity as Activity).runOnUiThread {
            }
        }

        override fun openQR() {
            (this@MainActivity as Activity).runOnUiThread {
                val intent = Intent(this@MainActivity, QrcodeScanActivity::class.java)

                val isFlash = SharedPreferencesUtil.getBoolean(this@MainActivity, SharedPreferencesUtil.Cmd.QR_FLASH)
                intent.putExtra(QrcodeScanActivity.IS_FLASH, isFlash)

                startActivityForResult(intent, REQUEST_QRSCAN_ACTIVITY)
            }
        }

        override fun isApp() {
            (this@MainActivity as Activity).runOnUiThread {
                mWebview?.sendEvent(IdevelServerScript.IS_APP, "android")
            }
        }
    }

    companion object {
        private val HANDLER_PERMISSIONS = 0
        private val HANDLER_NETWORK_TIMER = 1 // The network timer handler.
        private val HANDLER_SPLASH = 2 // 스플래시 종료 핸들러.
        private val HANDLER_SPLASH_DELAY = 3 // 스플래시 delay.

        private val DEV_REQUEST_CODE = 1000 // 히든 메뉴에서 돌아왔을 때 flag값.
        private val WEB_INTENT_REQUEST_CODE = 998
        private val TEL_REQUEST_CODE = 997
        private val PICK_FROM_ALBUM = 996
        private val PICK_FROM_CAMERA = 995
        private val REQUEST_QRSCAN_ACTIVITY = 994


        private val PICK_FROM_ALBUM_NEW = 999

        private val PING_TIME = 100000 //The ping time.
        private val NETWORK_CONNECTION_ERROR = 1 //The network connection error.
        private val TIMEOUT_ERROR = 2 //The timeout error.
        private val APP_VERSION_CHECK = 4 //앱 버전 check.
    }

    private class WeakHandler(act: MainActivity) : Handler() {
        private val ref: WeakReference<MainActivity> = WeakReference(act)

        override fun handleMessage(msg: Message) {
            val act = ref.get()

            if (act != null) {
                when (msg.what) {
                    HANDLER_NETWORK_TIMER -> {
                        act.showErrorDlg(TIMEOUT_ERROR)
                    }
                    HANDLER_SPLASH -> if (act.checkPermission()) {
                        act.setMainView()
                    }
                    HANDLER_SPLASH_DELAY -> {
                        act.showMainView()
                    }
                    HANDLER_PERMISSIONS -> {
                        act.showPermissionNoti()
                    }
                }
            }
        }
    }

    private val networkListener = object : NetworkChangeListener {
        override fun onNetworkDisconnected() {
            DLog.e("bjj Listener onNetworkDisconnected")

            // 네트워크 전환 시 onNetworkDisconnected 들어왔을 경우 1초 딜레이 후 네트워크 상태 체크하여 네트워크 차단팝업 발생하도록 함
            (this@MainActivity as Activity).runOnUiThread {
                Handler().postDelayed({
                    if (getNetworkInfo(applicationContext) == NETWORK_TYPE_ETC) {
                        showErrorDlg(NETWORK_CONNECTION_ERROR)
                    }
                }, 1000L)
            }
        }

        override fun onNetworkconnected() {
            DLog.e("bjj Listener onNetworkconnected")

            (this@MainActivity as Activity).runOnUiThread {
                Handler().postDelayed({
                    if (mAlertDialog?.isShowing == true) {
                        mAlertDialog?.dismiss()
                    }
                }, 300L)
            }
        }

        override fun onDataSaverChanged() {
            DLog.e("bjj Listener onDataSaverChanged")

//            (this@MainActivity as Activity).runOnUiThread {
//                showDataSaveDlg(R.string.popup_title_data_save, R.string.popup_msg_data_save)
//            }
        }
    }

    private var dataSaverListener = object : IDataSaverListener {
        override fun onDataSaverChanged() {
            DLog.e("bjj Listener onDataSaverChanged")

//            (this@MainActivity as Activity).runOnUiThread {
//                showDataSaveDlg(R.string.popup_title_data_save, R.string.popup_msg_data_save)
//            }
        }
    }

    // 현재 kakao만 적용됨
    private fun urlLoading(view: WebView?, uri: Uri?): Boolean {
        if (uri.toString().isNullOrEmpty()) {
            return false
        }

        val url = uri.toString()
        val scheme = uri!!.scheme

        DLog.e("bjj urlLoading scheme = $uri, ${uri!!.scheme}")

        when (scheme) {
            "https" -> return false
            "samsungapps" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try {
                        DLog.e("bjj urlLoading scheme = cc $url")

                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        startActivityForResult(intent, WEB_INTENT_REQUEST_CODE)
                    } catch (e: ActivityNotFoundException) {
                        DLog.e("bjj urlLoading scheme = dd com.samsung.android.spay ")

                        val marketIntent = Intent(Intent.ACTION_VIEW)
                        val urlSb = StringBuilder()
                        urlSb.append("market://details?id=").append("com.samsung.android.spay")
                        marketIntent.data = Uri.parse(urlSb.toString())
                        startActivityForResult(marketIntent, WEB_INTENT_REQUEST_CODE)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    try {
                        val isInstalled = isInstalledApp(this, "com.samsung.android.spay")

                        if (isInstalled) {
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            startActivityForResult(intent, WEB_INTENT_REQUEST_CODE)
                        } else {
                            val marketIntent = Intent(Intent.ACTION_VIEW)
                            val urlSb = StringBuilder()
                            urlSb.append("market://details?id=").append("com.samsung.android.spay")
                            marketIntent.data = Uri.parse(urlSb.toString())
                            startActivityForResult(marketIntent, WEB_INTENT_REQUEST_CODE)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                return true
            }
            "intent" -> {
                try {
                    // Intent 생성
                    val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)

                    // Fallback URL이 있으면 현재 웹뷰에 로딩
                    val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                    if (fallbackUrl != null) {
                        view?.loadUrl(fallbackUrl)

                        DLog.e("bjj urlLoading scheme intent FALLBACK: $fallbackUrl")
                    } else {
                        // 실행 가능한 앱이 있으면 앱 실행
                        if (intent.resolveActivity(packageManager) != null) {
                            startActivityForResult(intent, WEB_INTENT_REQUEST_CODE)

                            DLog.e("bjj urlLoading scheme intent ACTIVITY: ${intent.getPackage()}")
                        } else {
                            val marketIntent = Intent(Intent.ACTION_VIEW)
                            val urlSb = StringBuilder()

                            urlSb.append("market://details?id=").append(intent.getPackage())
                            marketIntent.data = Uri.parse(urlSb.toString())
                            startActivityForResult(marketIntent, WEB_INTENT_REQUEST_CODE)

                            DLog.e("bjj urlLoading scheme intent ACTIVITY: market ${intent.getPackage()}")
                        }
                    }
                } catch (e: URISyntaxException) {
                    DLog.e("bjj urlLoading scheme intent Invalid intent request", e)
                }

                return true
            }

            "market" -> {
                try {
                    val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                    if (intent != null) {
                        startActivityForResult(intent, WEB_INTENT_REQUEST_CODE)
                    }
                } catch (e: URISyntaxException) {
                    e.printStackTrace()
                }

                return true
            }

            "tel" -> {
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse(url))
                startActivityForResult(dialIntent, TEL_REQUEST_CODE)

                return true
            }

            // toss & pass
            "supertoss", "tauthlink", "ktauthexternalcall", "upluscorporation" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try {
                        DLog.e("bjj urlLoading scheme = aa $url")

                        val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                        startActivityForResult(intent, WEB_INTENT_REQUEST_CODE)
                    } catch (e: ActivityNotFoundException) {
                        DLog.e("bjj urlLoading scheme = bb ${intent.getPackage()}")

                        val marketIntent = Intent(Intent.ACTION_VIEW)
                        val urlSb = StringBuilder()
                        urlSb.append("market://details?id=").append(intent.getPackage())
                        marketIntent.data = Uri.parse(urlSb.toString())
                        startActivityForResult(marketIntent, WEB_INTENT_REQUEST_CODE)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    try {
                        val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                        val isInstalled = isInstalledApp(this, intent.getPackage())

                        if (isInstalled) {
                            startActivityForResult(intent, WEB_INTENT_REQUEST_CODE)
                        } else {
                            val marketIntent = Intent(Intent.ACTION_VIEW)
                            val urlSb = StringBuilder()
                            urlSb.append("market://details?id=").append(intent.getPackage())
                            marketIntent.data = Uri.parse(urlSb.toString())
                            startActivityForResult(marketIntent, WEB_INTENT_REQUEST_CODE)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                return true
            }
            else -> return false
        }
    }

    private fun isInstalledApp(context: Context, packageName: String?): Boolean {
        val appList = context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        for (appInfo in appList) {
            if (appInfo.packageName == packageName) {
                DLog.e("bjj urlLoading scheme isInstalledApp = true")
                return true
            }
        }

        DLog.e("bjj urlLoading scheme isInstalledApp = false")
        return false
    }

    /**
     * GPS
     */
    private fun getGPSLoacation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {

            Toast.makeText(this, "사용자의 위치 정보 권한을 허용하지 않았습니다", Toast.LENGTH_SHORT).show()
            return
        }

        if (mLocationManager == null) {
            mLocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        }

        mLocationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1f, mLocationListener)
        mLocationManager?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1f, mLocationListener)
    }

    private val mLocationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val geoCoder = Geocoder(this@MainActivity, Locale.getDefault())
            val addresses = geoCoder.getFromLocation(location!!.latitude, location!!.longitude, 1) // Here 1 represent max location result to returned, by documents it recommended 1 to 5

            DLog.d("bjj getGPSLoacation 00 : $addresses, ${addresses?.size}")

            if (addresses == null) {
                return
            }

            if (addresses.size == 0) {
                return
            }

            val address = addresses[0].getAddressLine(0) // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()

            val city = addresses[0].locality
            val state = addresses[0].adminArea
            val country = addresses[0].countryName
            val postalCode = addresses[0].postalCode
            val knownName = addresses[0].featureName // Only if available else return NULL

            val latitudeStr: String = java.lang.String.valueOf(location!!.latitude)
            val longitudeStr: String = java.lang.String.valueOf(location!!.longitude)

            if (location.provider == LocationManager.GPS_PROVIDER) {
                DLog.d("bjj getGPSLoacation aa : $address, ${latitudeStr}, ${longitudeStr}")
            } else {
                DLog.d("bjj getGPSLoacation bb : $address, ${latitudeStr}, ${longitudeStr}")
            }

            val gpsData = getLocationInfo(latitudeStr, longitudeStr, address)

            mWebview?.sendEvent(IdevelServerScript.GET_GPS_INFO, gpsData.toJsonString())
            mLocationManager?.removeUpdates(this)
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    @SuppressLint("StaticFieldLeak")
    private fun downloadFileTask(fileURL: String, fileName: String, type: String) {
        val baseUrl = "https://hansolhomedeco.wtest.biz"

        DLog.e("bjj download :: downloadFileTask :: start " + fileURL + " ^ " + fileName + " ^ " + type)

        mApiManager?.startRecordDownload(baseUrl, fileURL, object : OnResultListener<Any> {
            override fun onResult(result: Any, flag: Int) {
                recordDownloadTask?.cancel(true)
                recordDownloadTask = object : AsyncTask<Void, Void, Boolean>() {
                    override fun doInBackground(vararg voids: Void): Boolean {
                        DLog.e("bjj download :: downloadFileTask :: onResult " + fileName + " ^ " + Build.VERSION.SDK_INT)

                        return saveFile(result as ResponseBody, fileName, type, fileURL)
                    }

                    override fun onPostExecute(result: Boolean?) {
                        DLog.e("bjj download :: downloadFileTask :: onPostExecute " + fileName + " ^ " + result)
                    }
                }

                recordDownloadTask?.execute()
            }

            override fun onFail(error: Any, flag: Int) {
                DLog.e("bjj download :: downloadFileTask :: onFail " + fileName + " ^ " + error)
            }
        })
    }

    @Synchronized
    private fun saveFile(body: ResponseBody, fileName: String, type: String, pdfUrl: String): Boolean {
        val myBitmap = BitmapFactory.decodeStream(body.byteStream())

        try {
            var fos: OutputStream? = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val resolver: ContentResolver = this@MainActivity.contentResolver
                val contentValues = ContentValues()

                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)

                if (type.toLowerCase(Locale.ROOT) == "pdf") {
                    contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + File.separator + "hansolhomedeco")

                    val fileUri: Uri? = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    fos = resolver.openOutputStream(fileUri!!)

                    URL(pdfUrl).openStream().use { input ->
                        fos.use { output ->
                            input.copyTo(output!!, DEFAULT_BUFFER_SIZE)
                        }
                    }

                    MediaScannerConnection.scanFile(this@MainActivity, arrayOf(fileUri.toString()), null, null)

                    DLog.e("bjj download :: saveFile :: pdf " + type + " ^ " + fileUri.toString())
                } else {
                    contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + File.separator + "hansolhomedeco")

                    val fileUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    fos = resolver.openOutputStream(fileUri!!)

                    myBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)

                    MediaScannerConnection.scanFile(this@MainActivity, arrayOf(fileUri.toString()), null, null)

                    DLog.e("bjj download :: saveFile :: image " + type + " ^ " + fileUri.toString())
                }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + File.separator + "hansolhomedeco"
                val imagesDirFolder = File(imagesDir)

                if (!imagesDirFolder.exists()) {
                    imagesDirFolder.mkdir()
                }

                val imageFile: File

                if (type.toLowerCase(Locale.ROOT) == "pdf") {
                    imageFile = File(imagesDir, "$fileName.pdf")
                    fos = FileOutputStream(imageFile)

                    URL(pdfUrl).openStream().use { input ->
                        fos.use { output ->
                            input.copyTo(output!!, DEFAULT_BUFFER_SIZE)
                        }
                    }
                } else {
                    imageFile = File(imagesDir, "$fileName.jpg")
                    fos = FileOutputStream(imageFile)

                    myBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                }

                MediaScannerConnection.scanFile(this@MainActivity, arrayOf(imageFile.toString()), null, null)

                DLog.e("bjj download :: saveFile :: cc " + type + " ^ " + imageFile.toString())
            }

            (this@MainActivity as Activity).runOnUiThread {
                downloadDone(fileName, true)
            }

            fos?.flush()
            fos?.close()
        } catch (e: IOException) {
            e.printStackTrace()

            (this@MainActivity as Activity).runOnUiThread {
                downloadDone("", false)
            }

            DLog.e("bjj download :: saveFile ::  error " + e)
        }

        return false
    }

    private fun downloadDone(path: String, isSuccess: Boolean) {
        if (!path.isEmpty()) {
            Toast.makeText(this, "다운로드 완료.'" + path + "' 파일명으로 저장되었습니다", Toast.LENGTH_SHORT).show()
        }

        mWebview?.sendEvent(IdevelServerScript.SET_DOWNLOAD_FILE, DownloadFileStatusInfo(isSuccess).toJsonString())
    }

    private fun checkSchemeIntent(intent: Intent) {
        val isNoti = intent.getIntExtra(IS_NOTI, -1)

        DLog.e("bjj onNewIntent :: checkSchemeIntent init "
                + isNoti + " ^ "
//                + intent + " ^ "
//                + intent.data + " ^ "
                + intent.extras + " ^ "
//                + mWebview
        )

        if (isNoti == 1) {
            checkPushData(intent)
        } else if (isNoti == 0) {
            val linkType = intent.getStringExtra(PUSH_DATA_LINK_TYPE)
            val link = intent.getStringExtra(PUSH_DATA_LINK_URL)

            DLog.e("bjj onNewIntent :: checkSchemeIntent step_1 "
                    + mWebview + " ^ " + linkType + " ^ " + link)

            if (!linkType.isNullOrEmpty()) {
                if (linkType.contains("_webview")) {
                    isIntoNotiLandingUrl = true

                    if (mWebview != null) {
                        if (!link.isNullOrEmpty()) {
                            mHandler.postDelayed({
                                DLog.e("bjj onNewIntent :: checkSchemeIntent step_2 "
                                        + mWebview + " ^ " + linkType + " ^ " + link)

                                mWebview!!.loadUrl(link)
                            }, 5000L)
                        }
                    }
                }
            }
        } else {
            if (intent.extras != null) {
                val myExtras = intent.extras
                val myNotiStr = myExtras?.getString("noti")

                val pushDataNotiParser: PushDataNotiParser =
                        gson().fromJson(myNotiStr, PushDataNotiParser::class.java)

                val link: String? = pushDataNotiParser.noti_link
                val linkType: String? = pushDataNotiParser.noti_target

                DLog.e("bjj onNewIntent :: checkSchemeIntent step_3 "
                        + mWebview + " ^ " + linkType + " ^ " + link)

                if (!linkType.isNullOrEmpty()) {
                    if (linkType.contains("_webview")) {
                        isIntoNotiLandingUrl = true

                        if (mWebview != null) {
                            if (!link.isNullOrEmpty()) {
                                Toast.makeText(this@MainActivity, "화면 이동중입니다.", Toast.LENGTH_SHORT).show()

                                mHandler.postDelayed({
                                    DLog.e("bjj onNewIntent :: checkSchemeIntent step_4 "
                                            + mWebview + " ^ " + linkType + " ^ " + link)

                                    mWebview!!.loadUrl(link)
                                }, 5000L)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun gson(): Gson {
        return GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
    }

    private fun startTestBtn() {
        test_btn1 = findViewById(R.id.camera_test_btn)
        test_btn1?.visibility = View.GONE
        test_btn1?.setOnClickListener {
//            doTakePhotoAction("profile", "")
//            startBiometric()

//            startSpeech("en_US")

//            FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
//                if (!task.isSuccessful) {
//                    DLog.w("bjj FirebaseMessaging :: s " + task.exception)
//                    return@OnCompleteListener
//                }
//
//                // Get FCM registration token
//                val token = task.result
//                if (token != null) {
//                    DLog.w("bjj FirebaseMessaging :: f " + token)
//                }
//            })


//            iWebBridgeApi.openSharePopup("https://www.hansolhomedecolasola.com/simulation/simulation.php")

//            iWebBridgeApi.downloadFile("https://www.hansolhomedecolasola.com/upload/20220830/20220830_095335_8108.jpg", "img", "png")
        }

        test_btn2 = findViewById(R.id.gallery_test_btn)
        test_btn2?.visibility = View.GONE
        test_btn2?.setOnClickListener {
//            doTakeAlbumAction("profile", "")
//            startQr()

//            startSpeech("ko-KR")

//            iWebBridgeApi.isApp()

//            iWebBridgeApi.downloadFile("https://www.hansolhomedecolasola.com/upload/20220830/20220830_023410_8220.pdf", "pdff", "pdf")
        }
    }
}