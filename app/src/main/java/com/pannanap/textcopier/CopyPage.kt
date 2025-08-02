package com.pannanap.textcopier

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.pannanap.textcopier.databinding.FragmentCopyPageBinding // Import view binding class
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class CopyPage : Fragment() {

    private var _binding: FragmentCopyPageBinding? = null
    private val binding get() = _binding!!

    private val selectTextFileLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val fileName = getFileName(requireContext(), it)
                if (fileName != null && fileName.endsWith(".txt", ignoreCase = true)) {
                    copyFileContentToClipboard(it)
                } else if (fileName != null){
                    Toast.makeText(requireContext(), "Selected file is not a .txt file: $fileName", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(requireContext(), "Please select a .txt file.", Toast.LENGTH_LONG).show()
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCopyPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.button.setOnClickListener {
            selectTextFileLauncher.launch("text/plain")
        }
    }

    private fun copyFileContentToClipboard(uri: Uri) {
        try {
            val contentResolver = requireActivity().contentResolver
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val fileContent = reader.readText()

                    val clipboard =
                        requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Copied Text", fileContent)
                    clipboard.setPrimaryClip(clip)

                    Toast.makeText(
                        requireContext(),
                        "File content copied to clipboard!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } ?: run {
                Toast.makeText(requireContext(), "Failed to open file.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(
                requireContext(),
                "Error reading file: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var fileName: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        fileName = it.getString(nameIndex)
                    }
                }
            }
        }
        if (fileName == null) {
            fileName = uri.path
            val cut = fileName?.lastIndexOf('/')
            if (cut != -1 && cut != null) {
                fileName = fileName?.substring(cut + 1)
            }
        }
        return fileName
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
