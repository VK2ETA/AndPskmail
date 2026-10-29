/*
 * AppStorage.java
 *
 * Manages the SAF (Storage Access Framework) persistent URI for the user-selected
 * root folder. Exposes the resolved absolute path so that existing java.io.File
 * operations throughout the project continue to work unchanged.
 *
 * Folder layout created under the user-selected root:
 *   <UserSelectedFolder>/pskmail/
 *       Outbox/
 *       Pending/
 *       Outpending/
 *       Downloads/
 *
 * Compatible with Android 13+ (API 33) - uses SAF, no MANAGE_EXTERNAL_STORAGE needed.
 * Persists across app reinstalls via takePersistableUriPermission() +
 * Android Auto Backup of SharedPreferences.
 *
 * Call AppStorage.init(context) once from AndPskmail.onCreate().
 * Session.java and Processor.java use the no-Context static wrappers
 * (e.g. AppStorage.exists(path)) which delegate to the stored application context.
 */

package com.AndPskmail;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;

import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class AppStorage {

    // SharedPreferences key used to persist the tree URI string
    public static final String PREF_ROOT_URI = "SAF_ROOT_URI";

    // Sub-folder name created inside the user-selected root.
    // Matches the legacy Processor.Dirprefix folder name (without slashes).
    public static final String APP_SUBFOLDER = "pskmail";

    // -----------------------------------------------------------------------
    // Singleton — stores the application Context so that Session.java and
    // Processor.java can use the no-Context static wrappers without needing
    // to thread a Context through their entire call stacks.
    // -----------------------------------------------------------------------
    private static AppStorage sInstance;
    private Context mAppContext;

    /**
     * Must be called once from AndPskmail.onCreate() before any file I/O.
     * Safe to call again (e.g. after process restart).
     */
    public static void init(Context ctx) {
        if (sInstance == null) {
            sInstance = new AppStorage(ctx.getApplicationContext());
        } else {
            sInstance.mAppContext = ctx.getApplicationContext();
        }
    }

    public static AppStorage getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException("AppStorage.init(Context) must be called before use");
        }
        return sInstance;
    }

    private AppStorage(Context appContext) {
        mAppContext = appContext;
    }

    /** Returns the stored application context. Used by no-Context wrappers. */
    private static Context appCtx() {
        return getInstance().mAppContext;
    }

    // -----------------------------------------------------------------------
    // URI persistence helpers
    // -----------------------------------------------------------------------

    /**
     * Call after the user selects a folder via ACTION_OPEN_DOCUMENT_TREE.
     * Persists the URI so it survives process death and (via Auto Backup) reinstalls.
     */
    public static void persistRootUri(Context ctx, Uri treeUri) {
        final int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
        ctx.getContentResolver().takePersistableUriPermission(treeUri, flags);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        sp.edit().putString(PREF_ROOT_URI, treeUri.toString()).apply();
    }

    /**
     * Returns the persisted tree URI, or null if none has been selected yet.
     */
    public static Uri getPersistedRootUri(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        String uriString = sp.getString(PREF_ROOT_URI, "");
        if (uriString.isEmpty()) return null;
        return Uri.parse(uriString);
    }

    /**
     * Verifies that the system still holds a persistable URI permission.
     * Returns true if the app can access the folder without re-prompting.
     */
    public static boolean hasValidPermission(Context ctx) {
        Uri storedUri = getPersistedRootUri(ctx);
        if (storedUri == null) return false;

        ContentResolver cr = ctx.getContentResolver();
        List<UriPermission> perms = cr.getPersistedUriPermissions();
        for (UriPermission perm : perms) {
            if (perm.getUri().equals(storedUri)
                    && perm.isReadPermission()
                    && perm.isWritePermission()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Clears the stored URI (e.g. if the user wants to re-select the folder).
     */
    public static void clearPersistedUri(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        sp.edit().remove(PREF_ROOT_URI).apply();
    }

    // -----------------------------------------------------------------------
    // Path resolution - converts the SAF tree URI to a real filesystem path
    // so all existing java.io.File operations work unchanged.
    // -----------------------------------------------------------------------

    /**
     * Returns the absolute filesystem path of the user-selected parent folder.
     * Works for primary external storage (Downloads, Documents, etc.).
     * Returns empty string if the URI cannot be resolved to a real path.
     */
    public static String resolveAbsoluteParentPath(Context ctx) {
        Uri treeUri = getPersistedRootUri(ctx);
        if (treeUri == null) return "";

        String docId;
        try {
            docId = DocumentsContract.getTreeDocumentId(treeUri);
        } catch (Exception e) {
            return "";
        }

        if (docId.startsWith("primary:")) {
            // Primary external storage - maps to /sdcard/<relative>
            String relativePath = docId.substring("primary:".length());
            String basePath = Environment.getExternalStorageDirectory().getAbsolutePath();
            return basePath + File.separator + relativePath;
        }

        // Secondary volumes / SD cards: /storage/<volumeId>/<relative>
        if (docId.contains(":")) {
            String[] parts = docId.split(":", 2);
            return "/storage/" + parts[0] + File.separator + parts[1];
        }

        return "";
    }

    /**
     * Returns the DocumentFile for the RadioMsg.files directory,
     * creating it if it does not yet exist.
     * Returns null if the URI is missing or permission was revoked.
     */
    public static DocumentFile getOrCreateAppRootDocFile(Context ctx) {
        Uri treeUri = getPersistedRootUri(ctx);
        if (treeUri == null) return null;

        DocumentFile parent = DocumentFile.fromTreeUri(ctx, treeUri);
        if (parent == null || !parent.canWrite()) return null;

        DocumentFile appDir = parent.findFile(APP_SUBFOLDER);
        if (appDir == null || !appDir.isDirectory()) {
            appDir = parent.createDirectory(APP_SUBFOLDER);
        }
        return appDir;
    }

    /**
     * Returns (and creates if needed) a named sub-folder under RadioMsg.files/.
     */
    public static DocumentFile getOrCreateSubFolder(Context ctx, String subFolderName) {
        DocumentFile root = getOrCreateAppRootDocFile(ctx);
        if (root == null) return null;

        DocumentFile sub = root.findFile(subFolderName);
        if (sub == null || !sub.isDirectory()) {
            sub = root.createDirectory(subFolderName);
        }
        return sub;
    }

    // -----------------------------------------------------------------------
    // SAF I/O Helpers — drop-in replacements for java.io.File operations
    // -----------------------------------------------------------------------

    /**
     * Converts an absolute filesystem path to a SAF document Uri.
     * Used internally by stream helpers (openInputStream / openOutputStream).
     */
    public static Uri pathToDocumentUri(Context ctx, String absolutePath) {
        Uri treeUri = getPersistedRootUri(ctx);
        if (treeUri == null) return null;

        String rootPath = resolveAbsoluteParentPath(ctx);
        if (rootPath.isEmpty() || !absolutePath.startsWith(rootPath)) return null;

        String relativePath = absolutePath.substring(rootPath.length());
        if (relativePath.startsWith(File.separator)) {
            relativePath = relativePath.substring(1);
        }

        String treeDocId = DocumentsContract.getTreeDocumentId(treeUri);
        String docId = relativePath.isEmpty() ? treeDocId : treeDocId + "/" + relativePath;

        return DocumentsContract.buildDocumentUriUsingTree(treeUri, docId);
    }

    /**
     * Navigates the DocumentFile tree to the entry at absolutePath.
     * Returns null if not found.
     * Use for structural operations: exists(), delete(), listFiles(), isDirectory().
     */
    public static DocumentFile getDocumentFile(Context ctx, String absolutePath) {
        Uri treeUri = getPersistedRootUri(ctx);
        if (treeUri == null) return null;

        String rootPath = resolveAbsoluteParentPath(ctx);
        if (rootPath.isEmpty()) return null;
        if (!rootPath.endsWith(File.separator)) rootPath += File.separator;

        String rootStripped = rootPath.substring(0, rootPath.length() - 1);
        if (absolutePath.equals(rootStripped)) {
            return DocumentFile.fromTreeUri(ctx, treeUri);
        }

        if (!absolutePath.startsWith(rootPath)) return null;

        String[] segments = absolutePath.substring(rootPath.length()).split("/");
        DocumentFile current = DocumentFile.fromTreeUri(ctx, treeUri);
        for (String segment : segments) {
            if (segment.isEmpty() || current == null) continue;
            current = current.findFile(segment);
        }
        return current;
    }

    /**
     * Same as getDocumentFile() but creates missing directory segments.
     * Used by mkdirs() and openOutputStream() to ensure the path exists.
     */
    private static DocumentFile getOrCreateDocumentDir(Context ctx, String absolutePath) {
        Uri treeUri = getPersistedRootUri(ctx);
        if (treeUri == null) return null;

        String rootPath = resolveAbsoluteParentPath(ctx);
        if (rootPath.isEmpty()) return null;
        if (!rootPath.endsWith(File.separator)) rootPath += File.separator;

        String rootStripped = rootPath.substring(0, rootPath.length() - 1);
        if (absolutePath.equals(rootStripped)) {
            return DocumentFile.fromTreeUri(ctx, treeUri);
        }

        if (!absolutePath.startsWith(rootPath)) return null;

        String[] segments = absolutePath.substring(rootPath.length()).split("/");
        DocumentFile current = DocumentFile.fromTreeUri(ctx, treeUri);
        for (String segment : segments) {
            if (segment.isEmpty() || current == null) continue;
            DocumentFile next = current.findFile(segment);
            if (next == null || !next.isDirectory()) {
                next = current.createDirectory(segment);
            }
            current = next;
        }
        return current;
    }

    // --- Replaces: file.exists() ---
    public static boolean exists(Context ctx, String absolutePath) {
        DocumentFile df = getDocumentFile(ctx, absolutePath);
        return df != null && df.exists();
    }

    // --- Replaces: file.isDirectory() ---
    public static boolean isDirectory(Context ctx, String absolutePath) {
        DocumentFile df = getDocumentFile(ctx, absolutePath);
        return df != null && df.isDirectory();
    }

    // --- Replaces: file.mkdir() / file.mkdirs() ---
    public static boolean mkdirs(Context ctx, String absolutePath) {
        return getOrCreateDocumentDir(ctx, absolutePath) != null;
    }

    // --- Replaces: file.delete() ---
    public static boolean delete(Context ctx, String absolutePath) {
        DocumentFile df = getDocumentFile(ctx, absolutePath);
        return df != null && df.delete();
    }

    /**
     * Replaces: file.listFiles()
     * Returns absolute path strings so existing filename/path logic works unchanged.
     */
    public static String[] listFiles(Context ctx, String absolutePath) {
        DocumentFile df = getDocumentFile(ctx, absolutePath);
        if (df == null || !df.isDirectory()) return new String[0];

        DocumentFile[] children = df.listFiles();
        String base = absolutePath.endsWith(File.separator)
                ? absolutePath : absolutePath + File.separator;

        String[] paths = new String[children.length];
        for (int i = 0; i < children.length; i++) {
            paths[i] = base + children[i].getName();
        }
        return paths;
    }

    /**
     * Replaces: file.listFiles() where DocumentFile objects are needed directly.
     */
    public static DocumentFile[] listDocumentFiles(Context ctx, String absolutePath) {
        DocumentFile df = getDocumentFile(ctx, absolutePath);
        if (df == null || !df.isDirectory()) return new DocumentFile[0];
        return df.listFiles();
    }

    /**
     * Replaces: file.createNewFile()
     * Creates an empty file. Returns false if it already exists.
     */
    public static boolean createNewFile(Context ctx, String absolutePath) {
        if (exists(ctx, absolutePath)) return false;

        String parentPath = new File(absolutePath).getParent();
        String fileName   = new File(absolutePath).getName();
        if (parentPath == null) return false;

        DocumentFile parentDoc = getDocumentFile(ctx, parentPath);
        if (parentDoc == null || !parentDoc.isDirectory()) return false;

        return parentDoc.createFile(getMimeType(fileName), fileName) != null;
    }

    // --- Replaces: new FileInputStream(path) ---
    public static InputStream openInputStream(Context ctx, String absolutePath) throws IOException {
        Uri docUri = pathToDocumentUri(ctx, absolutePath);
        if (docUri == null) throw new IOException("Cannot resolve SAF URI for: " + absolutePath);
        InputStream is = ctx.getContentResolver().openInputStream(docUri);
        if (is == null) throw new IOException("Null stream returned for: " + absolutePath);
        return is;
    }

    // --- Replaces: new FileOutputStream(path) — truncates existing content ---
    public static OutputStream openOutputStream(Context ctx, String absolutePath) throws IOException {
        return openOutputStream(ctx, absolutePath, false);
    }

    // --- Replaces: new FileOutputStream(path, append) ---
    public static OutputStream openOutputStream(Context ctx, String absolutePath, boolean append)
            throws IOException {
        if (!exists(ctx, absolutePath)) {
            if (!createNewFile(ctx, absolutePath)) {
                throw new IOException("Cannot create SAF file at: " + absolutePath);
            }
        }
        Uri docUri = pathToDocumentUri(ctx, absolutePath);
        if (docUri == null) throw new IOException("Cannot resolve SAF URI for: " + absolutePath);
        String mode = append ? "wa" : "w";
        OutputStream os = ctx.getContentResolver().openOutputStream(docUri, mode);
        if (os == null) throw new IOException("Null stream returned for: " + absolutePath);
        return os;
    }

    // --- Infers MIME type from file extension (used by createNewFile) ---
    private static String getMimeType(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) return "application/octet-stream";
        switch (fileName.substring(dot + 1).toLowerCase()) {
            case "txt":  return "text/plain";
            case "log":  return "text/plain";
            case "csv":  return "text/csv";
            case "json": return "application/json";
            case "xml":  return "text/xml";
            case "jpg":
            case "jpeg": return "image/jpeg";
            case "png":  return "image/png";
            case "mp3":  return "audio/mpeg";
            case "wav":  return "audio/wav";
            case "pdf":  return "application/pdf";
            default:     return "application/octet-stream";
        }
    }

    // -----------------------------------------------------------------------
    // No-Context static convenience wrappers
    // These delegate to the stored application context so that Session.java
    // and Processor.java can call them without a Context parameter.
    // AppStorage.init(ctx) must have been called first.
    // -----------------------------------------------------------------------

    public static boolean hasValidPermission()              { return hasValidPermission(appCtx()); }
    public static Uri     getPersistedRootUri()             { return getPersistedRootUri(appCtx()); }
    public static void    persistRootUri(Uri treeUri)       { persistRootUri(appCtx(), treeUri); }
    public static void    clearPersistedUri()               { clearPersistedUri(appCtx()); }
    public static String  resolveAbsoluteParentPath()       { return resolveAbsoluteParentPath(appCtx()); }
    public static DocumentFile getOrCreateAppRootDocFile()  { return getOrCreateAppRootDocFile(appCtx()); }
    public static DocumentFile getOrCreateSubFolder(String sub) { return getOrCreateSubFolder(appCtx(), sub); }
    public static DocumentFile getDocumentFile(String path) { return getDocumentFile(appCtx(), path); }
    public static boolean  exists(String path)              { return exists(appCtx(), path); }
    public static boolean  isDirectory(String path)         { return isDirectory(appCtx(), path); }
    public static boolean  mkdirs(String path)              { return mkdirs(appCtx(), path); }
    public static boolean  delete(String path)              { return delete(appCtx(), path); }
    public static String[] listFiles(String path)           { return listFiles(appCtx(), path); }
    public static DocumentFile[] listDocumentFiles(String path) { return listDocumentFiles(appCtx(), path); }
    public static boolean  createNewFile(String path)       { return createNewFile(appCtx(), path); }

    public static InputStream openInputStream(String path) throws IOException {
        return openInputStream(appCtx(), path);
    }

    public static OutputStream openOutputStream(String path) throws IOException {
        return openOutputStream(appCtx(), path, false);
    }

    public static OutputStream openOutputStream(String path, boolean append) throws IOException {
        return openOutputStream(appCtx(), path, append);
    }

}
