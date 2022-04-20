package com.natlwd.palettesample

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.TextView.OnEditorActionListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.natlwd.palettesample.databinding.ActivitySearchImageBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import java.net.URLEncoder


class SearchImageActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchImageBinding
    private lateinit var adapter: PhotoGridAdapter

    companion object {
        const val TAG = "SearchImageActivity"

        fun startActivity(context: Context) {
            context.startActivity(Intent(context, SearchImageActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        setupView()
        setupListener()
    }

    private fun setupView() {
        this.adapter =  PhotoGridAdapter { item ->
            MainActivity.startActivity(this, item)
        }
        binding.photosGrid.adapter = this.adapter
    }

    private fun setupListener() {
        binding.searchImgButton.setOnClickListener {
            performSearch()
        }

        binding.searchImgEditText.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                return@OnEditorActionListener true
            }
            false
        })
    }

    private fun performSearch() {
        if (!binding.searchImgEditText.text.isNullOrBlank()) {
            getImages(binding.searchImgEditText.text.toString())
        } else {
            Toast.makeText(this, "image can't be empty", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getImages(searchQuery: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val encodedSearchUrl = "https://www.google.com/search?q=" + URLEncoder.encode(
                searchQuery,
                "UTF-8"
            ) + "&source=lnms&tbm=isch&sa=X&ved=0ahUKEwiUpP35yNXiAhU1BGMBHdDeBAgQ_AUIECgB"
            val doc = Jsoup.connect(encodedSearchUrl).get()
            val imgQuery = doc.select("img")

            val list = arrayListOf<ImageItem>()

            for (img in imgQuery) {
//                val s = img.attr("data-src")
                val s = img.absUrl("data-src")
                if (!s.isNullOrBlank()) {
                    list.add(ImageItem(url = s))
                }
                Log.d("image", s)
            }

            lifecycleScope.launch(Dispatchers.Main) {
                adapter.submitList(list)
            }
        }
    }
}