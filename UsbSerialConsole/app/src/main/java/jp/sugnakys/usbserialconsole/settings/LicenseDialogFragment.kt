package jp.sugnakys.usbserialconsole.settings

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.DialogFragment
import jp.sugnakys.usbserialconsole.R

class LicenseDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = activity?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val content = inflater.inflate(R.layout.license_view_main, null)

        val webView = content.findViewById<WebView>(R.id.webview)
        webView.webViewClient = WebViewClient()
        webView.loadUrl("file:///android_asset/license/license.html")

        return AlertDialog.Builder(activity)
                .setTitle(getString(R.string.license_title))
                .setView(content)
                .create()
    }
}
