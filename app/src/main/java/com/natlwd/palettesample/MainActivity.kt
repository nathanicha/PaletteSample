package com.natlwd.palettesample

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.palette.graphics.Palette
import com.github.dhaval2404.imagepicker.ImagePicker
import com.natlwd.palettesample.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

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
    }

    private fun resetColor() {
        binding.toolbar.setTitleTextColor(Color.BLACK)
        binding.toolbar.setBackgroundColor(Color.WHITE)
        binding.imageView.setBackgroundColor(Color.WHITE)

        val nullColor = AppCompatResources.getDrawable(this, R.drawable.shape_null_color)
        binding.colorImage0.background = nullColor
        binding.colorImage1.background = nullColor
        binding.colorImage2.background = nullColor
        binding.colorImage3.background = nullColor
        binding.colorImage4.background = nullColor
        binding.colorImage5.background = nullColor
        binding.colorImage6.background = nullColor
        binding.colorImage7.background = nullColor
    }

    private fun createPaletteAsync(bitmap: Bitmap) {
        resetColor()

        Palette.from(bitmap).generate() {
            it?.let { palette ->
                //gradient color
                val gd = GradientDrawable(
                    GradientDrawable.Orientation.BOTTOM_TOP, intArrayOf(
                        palette.darkMutedSwatch?.rgb ?: run {
                            palette.vibrantSwatch?.rgb ?: Color.BLACK
                        },
                        palette.lightMutedSwatch?.rgb ?: run {
                            palette.darkVibrantSwatch?.rgb ?: Color.BLACK
                        }
                    )
                )
                gd.cornerRadius = 0f
                //imageView bg color
                binding.imageView.background = gd
                //last color
                binding.colorImage7.background = gd

                palette.vibrantSwatch?.rgb?.let { vibrant ->
                    //first color
                    binding.colorImage0.setBackgroundColor(vibrant)
                    //toolbar bg color
                    binding.toolbar.setBackgroundColor(vibrant)
                }

                //toolbar title text color
                palette.vibrantSwatch?.titleTextColor?.let { vibrant ->
                    binding.toolbar.setTitleTextColor(vibrant)
                }

                palette.darkVibrantSwatch?.rgb?.let { darkVibrant ->
                    //second color
                    binding.colorImage1.setBackgroundColor(darkVibrant)
                }

                palette.lightVibrantSwatch?.rgb?.let { lightVibrant ->
                    //third color
                    binding.colorImage2.setBackgroundColor(lightVibrant)
                }

                palette.mutedSwatch?.rgb?.let { mutedSwatch ->
                    //fourth color
                    binding.colorImage3.setBackgroundColor(mutedSwatch)
                }

                palette.darkMutedSwatch?.rgb?.let { darkMutedSwatch ->
                    //fifth color
                    binding.colorImage4.setBackgroundColor(darkMutedSwatch)
                }

                palette.lightMutedSwatch?.rgb?.let { lightMutedSwatch ->
                    //sixth color
                    binding.colorImage5.setBackgroundColor(lightMutedSwatch)
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
}