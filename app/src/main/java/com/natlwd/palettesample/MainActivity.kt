package com.natlwd.palettesample

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.github.dhaval2404.imagepicker.ImagePicker
import com.natlwd.palettesample.databinding.ActivityMainBinding
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        const val TAG = "MainActivity"

        fun startActivity(context: Context, imageItem: ImageItem) {
            context.startActivity(Intent(context, MainActivity::class.java).apply {
                putExtra("image_item", imageItem)
            })
        }
    }

    private val startForProfileImageResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val resultCode = result.resultCode
            val data = result.data

            when (resultCode) {
                Activity.RESULT_OK -> {
                    //Image Uri will not be null for RESULT_OK
                    val fileUri = data?.data!!
                    getBitmap(fileUri)?.let { bitmap ->
                        binding.imageView.setImageBitmap(bitmap)
                        createPaletteAsync(bitmap)
                    }
                }
                ImagePicker.RESULT_ERROR -> {
                    Toast.makeText(this, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(this, "Task Cancelled", Toast.LENGTH_SHORT).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupView()
        setupListener()
    }

    private fun setupView() {
        val imageUrl = intent.getParcelableExtra<ImageItem>("image_item")
        imageUrl?.url?.let { url ->
//            Glide
//                .with(this)
//                .load(url)
//                .error(R.drawable.ic_error_outline)
//                .fitCenter()
//                .into(binding.imageView)

            getBitmapFromURL(url)?.let { bitmap ->
                binding.imageView.setImageBitmap(bitmap)
                createPaletteAsync(bitmap)
            } ?: run {
                Toast.makeText(this, "error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupListener() {
        binding.inputImageButton.setOnClickListener {
            ImagePicker.with(this)
                .compress(1024)         //Final image size will be less than 1 MB(Optional)
                .maxResultSize(
                    1080,
                    1080
                )  //Final image resolution will be less than 1080 x 1080(Optional)
                .createIntent { intent ->
                    startForProfileImageResult.launch(intent)
                }
        }

        binding.searchImageButton.setOnClickListener {
            SearchImageActivity.startActivity(this)
        }
    }

    private fun setGradient(p0: Int, p1: Int) {
        //gradient color
        val gd = GradientDrawable(
            GradientDrawable.Orientation.BOTTOM_TOP, intArrayOf(p0, p1)
        )
        gd.cornerRadius = 0f
        //imageView bg color
        binding.imageView.background = gd
        //last color
        binding.gradientImg.background = gd
    }

    private fun resetColor() {
        binding.toolbar.setTitleTextColor(Color.BLACK)
        binding.toolbar.setBackgroundColor(Color.WHITE)
        binding.imageView.setBackgroundColor(Color.WHITE)

        val nullColor = AppCompatResources.getDrawable(this, R.drawable.shape_null_color)
        binding.vibrantSwatchImg.background = nullColor
        binding.darkVibrantSwatchImg.background = nullColor
        binding.lightVibrantSwatchImg.background = nullColor
        binding.mutedSwatchImg.background = nullColor
        binding.darkMutedSwatchImg.background = nullColor
        binding.lightMutedSwatchImg.background = nullColor
        binding.gradientImg.background = nullColor
    }

    private fun createPaletteAsync(bitmap: Bitmap) {
        resetColor()

        Palette.from(bitmap).generate() {
            it?.let { palette ->
                /**
                 *  Palette
                 * */
                palette.vibrantSwatch?.rgb?.let { vibrant ->
                    //first color
                    binding.vibrantSwatchImg.setBackgroundColor(vibrant)
                    //toolbar bg color
                    binding.toolbar.setBackgroundColor(vibrant)
                }

                //toolbar title text color
                palette.vibrantSwatch?.titleTextColor?.let { vibrant ->
                    binding.toolbar.setTitleTextColor(vibrant)
                }

                palette.darkVibrantSwatch?.rgb?.let { darkVibrant ->
                    //second color
                    binding.darkVibrantSwatchImg.setBackgroundColor(darkVibrant)
                }

                palette.lightVibrantSwatch?.rgb?.let { lightVibrant ->
                    //third color
                    binding.lightVibrantSwatchImg.setBackgroundColor(lightVibrant)
                }

                palette.mutedSwatch?.rgb?.let { mutedSwatch ->
                    //fourth color
                    binding.mutedSwatchImg.setBackgroundColor(mutedSwatch)
                }

                palette.darkMutedSwatch?.rgb?.let { darkMutedSwatch ->
                    //fifth color
                    binding.darkMutedSwatchImg.setBackgroundColor(darkMutedSwatch)
                }

                palette.lightMutedSwatch?.rgb?.let { lightMutedSwatch ->
                    //sixth color
                    binding.lightMutedSwatchImg.setBackgroundColor(lightMutedSwatch)
                }

                /**
                 *  Gradient
                 * */
                val p0: Int?
                if (palette.darkMutedSwatch?.rgb != null) {
                    binding.darkMutedSwatchCheckBox.isChecked = true
                    p0 = palette.darkMutedSwatch?.rgb
                } else {
                    binding.vibrantSwatchCheckBox.isChecked = true
                    p0 = palette.vibrantSwatch?.rgb
                }

                val p1: Int?
                if (palette.lightMutedSwatch?.rgb != null) {
                    binding.lightMutedSwatchCheckBox.isChecked = true
                    p1 = palette.lightMutedSwatch?.rgb
                } else {
                    binding.darkVibrantSwatchCheckBox.isChecked = true
                    p1 = palette.darkVibrantSwatch?.rgb
                }

                if (p0 != null && p1 != null) {
                    setGradient(p0, p1)
                }
            }
        }
    }

    private fun getBitmap(uri: Uri): Bitmap? {
        val contentResolver = contentResolver
        return try {
            val bitmap = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            } else {
                val source: ImageDecoder.Source = ImageDecoder.createSource(contentResolver, uri)
//                ImageDecoder.decodeBitmap(source).copy(Bitmap.Config.RGBA_F16, true)
                ImageDecoder.decodeBitmap(
                    source
                ) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                }
            }

            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getBitmapFromURL(src: String?): Bitmap? {
        return try {
            val url = URL(src)
            val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input: InputStream = connection.inputStream
            BitmapFactory.decodeStream(input)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}