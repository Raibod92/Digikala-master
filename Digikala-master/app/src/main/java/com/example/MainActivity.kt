package com.example

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.Brush
import com.example.ui.theme.DigikalaBorder
import com.example.ui.theme.DigikalaGeometricBg
import com.example.ui.theme.DigikalaRed
import com.example.ui.theme.DigikalaRedDark
import com.example.ui.theme.DigikalaRedLight
import com.example.ui.theme.DigikalaTextDark
import com.example.ui.theme.DigikalaTextGray
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val DIGIKALA_URL = "https://www.digikala.com"

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    AppInitializer.initialize(this)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        DigikalaApp()
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DigikalaApp() {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var webView by remember { mutableStateOf<WebView?>(null) }
  var canGoBack by remember { mutableStateOf(false) }
  var isLoading by remember { mutableStateOf(true) }
  var isInitialSplash by remember { mutableStateOf(true) }
  var loadProgress by remember { mutableFloatStateOf(0f) }
  var hasError by remember { mutableStateOf(false) }
  var isRefreshing by remember { mutableStateOf(false) }

  var fileUploadCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

  val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) { result ->
    val callback = fileUploadCallback
    fileUploadCallback = null
    if (callback != null) {
      if (result.resultCode == Activity.RESULT_OK && result.data != null) {
        val data = result.data
        val uris: Array<Uri>? = when {
          data?.clipData != null -> {
            val clip = data.clipData!!
            Array(clip.itemCount) { i -> clip.getItemAt(i).uri }
          }
          data?.data != null -> arrayOf(data.data!!)
          else -> null
        }
        callback.onReceiveValue(uris)
      } else {
        callback.onReceiveValue(null)
      }
    }
  }

  // Handle hardware / gesture back button
  BackHandler(enabled = canGoBack && !hasError) {
    webView?.let { wv ->
      if (wv.canGoBack()) {
        wv.goBack()
      }
    }
  }

  // Pull to refresh state
  val pullToRefreshState = rememberPullToRefreshState()

  var showPromoDialog by remember { mutableStateOf(false) }
  var isBannerVisible by remember { mutableStateOf(true) }

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.White)
      .statusBarsPadding()
      .navigationBarsPadding(),
    containerColor = Color.White
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .testTag("digikala_main_container")
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // Top Geometric Promo Announcement Banner
        AnimatedVisibility(
          visible = isBannerVisible && !isInitialSplash,
          enter = fadeIn(),
          exit = fadeOut()
        ) {
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { showPromoDialog = true }
              .testTag("promo_top_banner"),
            color = DigikalaRedDark,
            shadowElevation = 4.dp
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .background(
                  brush = Brush.horizontalGradient(
                    colors = listOf(DigikalaRedDark, DigikalaRed, DigikalaRedLight)
                  )
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.weight(1f)
                ) {
                  Icon(
                    imageVector = Icons.Default.CardGiftcard,
                    contentDescription = null,
                    tint = Color(0xFFFFE082),
                    modifier = Modifier.size(20.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "🔥 ورژن جدید: ۵ خرید از این به بعد کاملاً رایگان!! 🔥",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                  )
                }

                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.25f))
                    .clickable { showPromoDialog = true }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                  Text(
                    text = "مشاهده هدیه",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }
        }

        // Main WebView container wrapped in PullToRefresh
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
          PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
              isRefreshing = true
              hasError = false
              webView?.reload()
              scope.launch {
                delay(1500)
                isRefreshing = false
              }
            },
            state = pullToRefreshState,
            modifier = Modifier.fillMaxSize()
          ) {
            AndroidView(
              modifier = Modifier
                .fillMaxSize()
                .testTag("digikala_webview"),
              factory = { ctx ->
                WebView(ctx).apply {
                  layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                  )
                  setBackgroundColor(android.graphics.Color.WHITE)
                  setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

                  settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    setSupportZoom(true)
                    builtInZoomControls = false
                    displayZoomControls = false
                    cacheMode = WebSettings.LOAD_DEFAULT
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    mediaPlaybackRequiresUserGesture = false
                    loadsImagesAutomatically = true
                    defaultTextEncodingName = "UTF-8"

                    // User agent enhancement for optimal native-like rendering
                    val defaultUa = userAgentString
                    userAgentString = "$defaultUa Mobile DigikalaApp/1.0"
                  }

                  // Enable cookies
                  val cookieManager = CookieManager.getInstance()
                  cookieManager.setAcceptCookie(true)
                  cookieManager.setAcceptThirdPartyCookies(this, true)

                  webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                      super.onProgressChanged(view, newProgress)
                      loadProgress = newProgress / 100f
                      if (newProgress >= 100) {
                        isLoading = false
                        isRefreshing = false
                        isInitialSplash = false
                      }
                    }

                    override fun onGeolocationPermissionsShowPrompt(
                      origin: String?,
                      callback: GeolocationPermissions.Callback?
                    ) {
                      callback?.invoke(origin, true, false)
                    }

                    override fun onShowFileChooser(
                      mWebView: WebView?,
                      filePathCallback: ValueCallback<Array<Uri>>?,
                      fileChooserParams: FileChooserParams?
                    ): Boolean {
                      fileUploadCallback?.onReceiveValue(null)
                      fileUploadCallback = filePathCallback

                      val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "*/*"
                        addCategory(Intent.CATEGORY_OPENABLE)
                      }

                      try {
                        filePickerLauncher.launch(intent)
                        return true
                      } catch (e: Exception) {
                        fileUploadCallback = null
                        return false
                      }
                    }
                  }

                  webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                      super.onPageStarted(view, url, favicon)
                      isLoading = true
                      hasError = false
                      canGoBack = view?.canGoBack() == true
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                      super.onPageFinished(view, url)
                      isLoading = false
                      isRefreshing = false
                      canGoBack = view?.canGoBack() == true
                      isInitialSplash = false
                    }

                    override fun onReceivedError(
                      view: WebView?,
                      request: WebResourceRequest?,
                      error: WebResourceError?
                    ) {
                      super.onReceivedError(view, request, error)
                      if (request?.isForMainFrame == true) {
                        hasError = true
                        isLoading = false
                        isRefreshing = false
                      }
                    }

                    override fun shouldOverrideUrlLoading(
                      view: WebView?,
                      request: WebResourceRequest?
                    ): Boolean {
                      val url = request?.url?.toString() ?: return false

                      // Handle external schemes (tel, sms, mailto, intent, telegram, payment return apps)
                      return if (url.startsWith("http://") || url.startsWith("https://")) {
                        false // Let the WebView load http/https links
                      } else {
                        try {
                          val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                          ctx.startActivity(intent)
                          true
                        } catch (e: Exception) {
                          true
                        }
                      }
                    }
                  }

                  loadUrl(DIGIKALA_URL)
                  webView = this
                }
              },
              update = { wv ->
                webView = wv
                canGoBack = wv.canGoBack()
              }
            )
          }
        }
      }

      // Floating Promo Badge Pill (Bottom Start)
      if (!isInitialSplash && !hasError) {
        Box(
          modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(start = 16.dp, bottom = 24.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
              brush = Brush.horizontalGradient(
                colors = listOf(DigikalaRedDark, DigikalaRed)
              )
            )
            .border(width = 1.5.dp, color = Color(0xFFFFD54F), shape = RoundedCornerShape(24.dp))
            .clickable { showPromoDialog = true }
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("floating_promo_pill")
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Stars,
              contentDescription = null,
              tint = Color(0xFFFFD54F),
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "🎁 ۵ خرید رایگان فعال شد!",
              color = Color.White,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      // Top linear progress bar during navigation/page load
      if (isLoading && !isInitialSplash && !hasError) {
        LinearProgressIndicator(
          progress = { loadProgress },
          modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .align(Alignment.TopCenter)
            .testTag("digikala_progress_bar"),
          color = DigikalaRed,
          trackColor = Color(0x33EF394E)
        )
      }

      // Error / Offline Screen
      if (hasError) {
        DigikalaErrorScreen(
          onRetry = {
            hasError = false
            isLoading = true
            webView?.reload() ?: webView?.loadUrl(DIGIKALA_URL)
          }
        )
      }

      // Initial Brand Splash Overlay
      AnimatedVisibility(
        visible = isInitialSplash,
        enter = fadeIn(),
        exit = fadeOut(animationSpec = tween(500)),
        modifier = Modifier.fillMaxSize()
      ) {
        DigikalaSplashScreen()
      }

      // Celebratory Promo Dialog
      if (showPromoDialog) {
        DigikalaPromoDialog(onDismiss = { showPromoDialog = false })
      }
    }
  }

  // Clean up webView on dispose
  DisposableEffect(Unit) {
    onDispose {
      webView?.destroy()
    }
  }
}

@Composable
fun DigikalaPromoDialog(onDismiss: () -> Unit) {
  AlertDialog(
    onDismissRequest = onDismiss,
    modifier = Modifier
      .fillMaxWidth(0.92f)
      .clip(RoundedCornerShape(28.dp))
      .testTag("digikala_promo_dialog"),
    containerColor = Color.White,
    titleContentColor = DigikalaTextDark,
    textContentColor = DigikalaTextDark,
    title = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Box(
          modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(
              brush = Brush.linearGradient(listOf(DigikalaRed, DigikalaRedLight))
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.CardGiftcard,
            contentDescription = null,
            tint = Color(0xFFFFD54F),
            modifier = Modifier.size(36.dp)
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "🎉 ورژن جدید فعال شد!",
          fontSize = 20.sp,
          fontWeight = FontWeight.Black,
          color = DigikalaRed,
          textAlign = TextAlign.Center
        )
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Highlighting badge
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFFF0F2))
            .border(width = 1.dp, color = Color(0xFFFFD5DA), shape = RoundedCornerShape(16.dp))
            .padding(14.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "🎁 ۵ خرید از این به بعد کاملاً رایگان!! 🎁",
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            color = DigikalaRedDark,
            textAlign = TextAlign.Center
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "به مناسبت انتشار ورژن جدید دیجی‌کالا، تخفیف ۱۰۰٪ برای ۵ خرید بعدی شما از کل برنامه و تمام دسته‌بندی‌ها فعال شد!",
          fontSize = 13.sp,
          color = DigikalaTextDark,
          textAlign = TextAlign.Center,
          lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 5 Purchases Badges Row
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          for (i in 1..5) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFE8F5E9))
                .border(1.dp, Color(0xFFC8E6C9), RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
              Text(
                text = "سفارش $i: رایگان ✓",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("dismiss_promo_button"),
        colors = ButtonDefaults.buttonColors(
          containerColor = DigikalaRed,
          contentColor = Color.White
        ),
        shape = RoundedCornerShape(14.dp)
      ) {
        Text(
          text = "شروع خرید با ۵ سفارش رایگان 🛍️",
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }
  )
}

@Composable
fun DigikalaSplashScreen() {
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val scale by infiniteTransition.animateFloat(
    initialValue = 0.97f,
    targetValue = 1.03f,
    animationSpec = infiniteRepeatable(
      animation = tween(1000, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "logoScale"
  )

  Surface(
    modifier = Modifier
      .fillMaxSize()
      .testTag("digikala_splash_screen")
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          brush = Brush.linearGradient(
            colors = listOf(DigikalaRed, DigikalaRedLight)
          )
        ),
      contentAlignment = Alignment.Center
    ) {
      // Geometric background decorative elements
      Box(
        modifier = Modifier
          .offset(x = 100.dp, y = (-120).dp)
          .size(240.dp)
          .clip(CircleShape)
          .background(Color.White.copy(alpha = 0.08f))
      )
      Box(
        modifier = Modifier
          .offset(x = (-90).dp, y = 140.dp)
          .size(200.dp)
          .clip(CircleShape)
          .background(Color.White.copy(alpha = 0.06f))
      )

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(20.dp)
      ) {
        // Geometric card emblem
        Box(
          modifier = Modifier
            .size(100.dp)
            .scale(scale)
            .clip(RoundedCornerShape(26.dp))
            .background(Color.White)
            .border(width = 1.5.dp, color = Color.White.copy(alpha = 0.9f), shape = RoundedCornerShape(26.dp))
            .padding(12.dp),
          contentAlignment = Alignment.Center
        ) {
          Image(
            painter = painterResource(id = R.drawable.digikala_logo),
            contentDescription = stringResource(id = R.string.app_name),
            modifier = Modifier.size(76.dp)
          )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
          text = stringResource(id = R.string.app_name),
          color = Color.White,
          fontSize = 26.sp,
          fontWeight = FontWeight.Black,
          letterSpacing = 1.5.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // New version badge
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.2f))
            .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
          Text(
            text = "🚀 ورژن جدید کل برنامه فعال شد!",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 5 Purchases Free announcement badge
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
          Text(
            text = "🎁 ۵ خرید از این به بعد کاملاً رایگان!! 🎁",
            color = DigikalaRed,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black
          )
        }

        Spacer(modifier = Modifier.height(28.dp))

        CircularProgressIndicator(
          modifier = Modifier.size(30.dp),
          color = Color.White,
          strokeWidth = 3.dp
        )
      }
    }
  }
}

@Composable
fun DigikalaErrorScreen(onRetry: () -> Unit) {
  Surface(
    modifier = Modifier
      .fillMaxSize()
      .testTag("digikala_error_screen"),
    color = DigikalaGeometricBg
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(20.dp),
      contentAlignment = Alignment.Center
    ) {
      // Geometric card layout
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(28.dp))
          .background(Color.White)
          .border(width = 1.dp, color = DigikalaBorder, shape = RoundedCornerShape(28.dp))
          .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Box(
          modifier = Modifier
            .size(76.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFFFFF0F2))
            .border(width = 1.dp, color = Color(0xFFFFD5DA), shape = RoundedCornerShape(22.dp)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.WifiOff,
            contentDescription = null,
            tint = DigikalaRed,
            modifier = Modifier.size(36.dp)
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
          text = stringResource(id = R.string.no_internet_title),
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = DigikalaTextDark,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = stringResource(id = R.string.no_internet_description),
          style = MaterialTheme.typography.bodyMedium,
          color = DigikalaTextGray,
          textAlign = TextAlign.Center,
          lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Reminder badge
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFF3E0))
            .padding(8.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "✨ هدیه نسخه جدید: ۵ خرید اول شما ۱۰۰٪ رایگان است!",
            color = Color(0xFFE65100),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
          onClick = onRetry,
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("retry_button"),
          colors = ButtonDefaults.buttonColors(
            containerColor = DigikalaRed,
            contentColor = Color.White
          ),
          shape = RoundedCornerShape(14.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.size(8.dp))
          Text(
            text = stringResource(id = R.string.retry),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}

