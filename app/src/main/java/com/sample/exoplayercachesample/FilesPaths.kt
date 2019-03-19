package com.sample.exoplayercachesample

import android.content.Context
import java.io.File

enum class FilesPaths {
    VIDEO_CACHE_FOLDER,FILE_TO_SHARE;

    fun getFile(context: Context, deleteIfExisting: Boolean): File {
        return when (this) {
            VIDEO_CACHE_FOLDER -> {
                val result = File(context.cacheDir, "caller_id_clip_sound_preview")
                result.mkdirs()
                result
            }
            FILE_TO_SHARE -> prepareFile(context, "", "temp_vid.mp4", false, deleteIfExisting)
        }
    }

    private fun prepareFileWithTimestamp(context: Context, parentFileName: String? = null, fileNameFormat: String, isExternal: Boolean = false, alsoCreateNoMediaFileForExternalFolder: Boolean = true): File {
        var id = System.currentTimeMillis()
        val parentFile = getParentFile(context, isExternal, parentFileName)
        parentFile.mkdirs()
        if (isExternal && alsoCreateNoMediaFileForExternalFolder)
            File(parentFile, ".nomedia").createNewFile()
        var file: File
        while (true) {
            val fileName = fileNameFormat.format(id.toString())
            file = File(parentFile, fileName)
            if (file.exists())
                ++id
            else
                return file
        }
    }

    private fun getParentFile(context: Context, isExternal: Boolean = false, parentFileName: String?): File {
        if (isExternal) {
            val parentFile: File? = context.getExternalFilesDir(parentFileName)
            if (parentFile != null)
                return parentFile
        }
        if (parentFileName == null)
            return context.filesDir!!
        return File(context.filesDir!!, parentFileName)
    }

    private fun prepareFile(context: Context, parentFileName: String? = null, fileName: String, isExternal: Boolean = false, deleteIfExisting: Boolean, alsoCreateNoMediaFileForExternalFolder: Boolean = true): File {
        val parentFile = getParentFile(context, isExternal, parentFileName)
        parentFile.mkdirs()
        if (isExternal && alsoCreateNoMediaFileForExternalFolder)
            File(parentFile, ".nomedia").createNewFile()
        val file = File(parentFile, fileName)
        if (deleteIfExisting)
            file.delete()
        return file
    }
}
