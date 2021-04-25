package com.simsim.island.util

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import java.io.FileOutputStream


/**
 * Get a file path from a Uri. This will get the the path for Storage Access
 * Framework Documents, as well as the _data field for the MediaStore and
 * other file-based ContentProviders.<br></br>
 * <br></br>
 * Callers should check whether the path is local before assuming it
 * represents a local file.
 *
 * @param context The context.
 * @param uri     The Uri to query.
 */
@SuppressLint("NewApi", "ObsoleteSdkInt")
fun getPath(context: Context, uri: Uri): String? {
    // check here to KITKAT or new version
    val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
    val selection: String?
    val selectionArgs: Array<String>?
    // DocumentProvider
    if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
        // ExternalStorageProvider
        if (isExternalStorageDocument(uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split(":".toRegex()).toTypedArray()
//            val type = split[0]
            val fullPath = getPathFromExtSD(split)
            return if (fullPath !== "") {
                fullPath
            } else {
                null
            }
        } else if (isDownloadsDocument(uri)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    context.contentResolver
                        .query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)?.use { cursor->
                            cursor.moveToFirst()
                            val fileName: String = cursor.getString(0)
                            val path: String = Environment.getExternalStorageDirectory().toString() + "/Download/" + fileName
                            if (path.isNotBlank()) {
                                return path
                            }
                        }}catch (e:Exception){
                            Log.e("getPath",e.stackTraceToString())
                        }
                val id: String = DocumentsContract.getDocumentId(uri)
                if (id.isNotBlank()) {
                    if (id.startsWith("raw:")) {
                        return id.replaceFirst("raw:".toRegex(), "")
                    }
                    val contentUriPrefixesToTry = arrayOf(
                        "content://downloads/public_downloads",
                        "content://downloads/my_downloads"
                    )
                    for (contentUriPrefix in contentUriPrefixesToTry) {
                        return try {
                            val contentUri: Uri = ContentUris.withAppendedId(
                                Uri.parse(contentUriPrefix),
                                id.toLong()
                            )

                            /*   final Uri contentUri = ContentUris.withAppendedId(
                                                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));*/
                            getDataColumn(context, contentUri, null, null)
                        } catch (e: NumberFormatException) {
                            //In Android 8 and Android P the id is not a number
                            uri.path?.replaceFirst("^/document/raw:", "")
                                ?.replaceFirst("^raw:", "")
                        }
                    }
                }
            } else {
                val id = DocumentsContract.getDocumentId(uri)
//                val isOreo = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                if (id.startsWith("raw:")) {
                    return id.replaceFirst("raw:".toRegex(), "")
                }
                val contentUri=try {
                    ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        java.lang.Long.valueOf(id)
                    )
                } catch (e: NumberFormatException) {
                    e.printStackTrace()
                    null
                }
                if (contentUri != null) {
                    return getDataColumn(context, contentUri, null, null)
                }
            }
        } else if (isMediaDocument(uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split(":".toRegex()).toTypedArray()
            val contentUri=when(split[0]){
                "image"->{MediaStore.Images.Media.EXTERNAL_CONTENT_URI}
                "video"->{MediaStore.Video.Media.EXTERNAL_CONTENT_URI}
                "audio"->{MediaStore.Audio.Media.EXTERNAL_CONTENT_URI}
                else->null
            }
            selection = "_id=?"
            selectionArgs = arrayOf(split[1])
            return getDataColumn(
                context, contentUri, selection,
                selectionArgs
            )
        } else if (isGoogleDriveUri(uri)) {
            return getDriveFilePath(uri, context)
        }
    } else if ("content".equals(uri.scheme, ignoreCase = true)) {
        if (isGooglePhotosUri(uri)) {
            return uri.lastPathSegment
        }
        if (isGoogleDriveUri(uri)) {
            return getDriveFilePath(uri, context)
        }
        return if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N) {
            // return getFilePathFromURI(context,uri);
            getMediaFilePathForN(uri, context)
            // return getRealPathFromURI(context,uri);
        } else {
            getDataColumn(context, uri, null, null)
        }
    } else if ("file".equals(uri.scheme, ignoreCase = true)) {
        return uri.path
    }
    return null
}

/**
 * Check if a file exists on device
 *
 * @param filePath The absolute file path
 */
private fun fileExists(filePath: String): Boolean {
    val file = File(filePath)
    return file.exists()
}


/**
 * Get full file path from external storage
 *
 * @param pathData The storage type and the relative path
 */
private fun getPathFromExtSD(pathData: Array<String>): String {
    val type = pathData[0]
    val relativePath = "/" + pathData[1]
    var fullPath: String

    // on my Sony devices (4.4.4 & 5.1.1), `type` is a dynamic string
    // something like "71F8-2C0A", some kind of unique id per storage
    // don't know any API that can get the root path of that storage based on its id.
    //
    // so no "primary" type, but let the check here for other devices
    if ("primary".equals(type, ignoreCase = true)) {
        fullPath = Environment.getExternalStorageDirectory().toString() + relativePath
        if (fileExists(fullPath)) {
            return fullPath
        }
    }

    // Environment.isExternalStorageRemovable() is `true` for external and internal storage
    // so we cannot relay on it.
    //
    // instead, for each possible path, check if file exists
    // we'll start with secondary storage as this could be our (physically) removable sd card
    fullPath = System.getenv("SECONDARY_STORAGE")?.plus(relativePath)?:""
    if (fileExists(fullPath)) {
        return fullPath
    }
    fullPath = System.getenv("EXTERNAL_STORAGE")?.plus(relativePath)?:""
    return if (fileExists(fullPath)) {
        fullPath
    } else fullPath
}

private fun getDriveFilePath(uri: Uri, context: Context): String? {
    /*
     * Get the column indexes of the data in the Cursor,
     *     * move to the first row in the Cursor, get the data,
     *     * and display it.
     * */
    val returnUri: Uri = uri
    return context.contentResolver.query(returnUri, null, null, null, null)?.use { returnCursor->
        val nameIndex: Int = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
//        val sizeIndex: Int = returnCursor.getColumnIndex(OpenableColumns.SIZE)
        returnCursor.moveToFirst()
        val name: String = returnCursor.getString(nameIndex)
//        val size = returnCursor.getLong(sizeIndex).toString()
        val file = File(context.cacheDir, name)
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream->
                FileOutputStream(file).use { outputStream->
//                    var read = 0
//                    val maxBufferSize = 1 * 1024 * 1024
//                    val bytesAvailable: Int = inputStream.available()
//                    val bufferSize = bytesAvailable.coerceAtMost(maxBufferSize)
//                    val buffers = ByteArray(bufferSize)
//                    while (inputStream.read(buffers).also { read = it } != -1) {
//                        outputStream.write(buffers, 0, read)
//                    }
                    outputStream.write(inputStream.readBytes())
                    Log.e("File Size", "Size " + file.length())
                    Log.e("File Path", "Path " + file.path)
                    Log.e("File Size", "Size " + file.length())
                }
            }
        }catch (e: Exception) {
            Log.e("Exception", e.stackTraceToString())
        }
        file.path
    }

}

private fun getMediaFilePathForN(uri: Uri, context: Context): String? {

    val returnUri: Uri = uri
    return context.contentResolver.query(returnUri, null, null, null, null)?.use { returnCursor->
        val nameIndex: Int = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
//        val sizeIndex: Int = returnCursor.getColumnIndex(OpenableColumns.SIZE)
        returnCursor.moveToFirst()
        val name: String = returnCursor.getString(nameIndex)
//        val size = returnCursor.getLong(sizeIndex).toString()
        val file = File(context.filesDir, name)
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream->
                FileOutputStream(file).use { outputStream->
//                    var read = 0
//                    val maxBufferSize = 1 * 1024 * 1024
//                    val bytesAvailable: Int = inputStream.available()
//                    val bufferSize = bytesAvailable.coerceAtMost(maxBufferSize)
//                    val buffers = ByteArray(bufferSize)
//                    while (inputStream.read(buffers).also { read = it } != -1) {
//                        outputStream.write(buffers, 0, read)
//                    }
                    outputStream.write(inputStream.readBytes())
                    Log.e("File Size", "Size " + file.length())
                    Log.e("File Path", "Path " + file.path)
                    Log.e("File Size", "Size " + file.length())
                }
            }
        }catch (e: Exception) {
            Log.e("Exception", e.stackTraceToString())
        }
        file.path
    }
}


private fun getDataColumn(
    context: Context, uri: Uri?,
    selection: String?, selectionArgs: Array<String>?
): String? {
    val column = "_data"
    val projection = arrayOf(column)
    return uri?.let {
        context.contentResolver.query(
            uri, projection,
            selection, selectionArgs, null
        )?.use { cursor->
            cursor.moveToFirst()
            val index: Int = cursor.getColumnIndexOrThrow(column)
            cursor.getString(index)
        }
    }
}

/**
 * @param uri - The Uri to check.
 * @return - Whether the Uri authority is ExternalStorageProvider.
 */
private fun isExternalStorageDocument(uri: Uri): Boolean {
    return "com.android.externalstorage.documents" == uri.authority
}

/**
 * @param uri - The Uri to check.
 * @return - Whether the Uri authority is DownloadsProvider.
 */
private fun isDownloadsDocument(uri: Uri): Boolean {
    return "com.android.providers.downloads.documents" == uri.authority
}

/**
 * @param uri - The Uri to check.
 * @return - Whether the Uri authority is MediaProvider.
 */
private fun isMediaDocument(uri: Uri): Boolean {
    return "com.android.providers.media.documents" == uri.authority
}

/**
 * @param uri - The Uri to check.
 * @return - Whether the Uri authority is Google Photos.
 */
private fun isGooglePhotosUri(uri: Uri): Boolean {
    return "com.google.android.apps.photos.content" == uri.authority
}


/**
 * @param uri The Uri to check.
 * @return Whether the Uri authority is Google Drive.
 */
private fun isGoogleDriveUri(uri: Uri): Boolean {
    return "com.google.android.apps.docs.storage" == uri.authority || "com.google.android.apps.docs.storage.legacy" == uri.authority
}