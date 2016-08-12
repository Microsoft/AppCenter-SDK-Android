package avalanche.core;

import android.content.Context;

import java.io.File;

import avalanche.core.utils.AvalancheLog;

/**
 * <h3>Description</h3>
 *
 * Various constants and meta information loaded from the context.
 **/
public class Constants {

    /**
     * Path where crash logs and temporary files are stored.
     */
    public static String FILES_PATH = null;

    /**
     * Initializes constants from the given context. The context is used to set
     * the package name, version code, and the files path.
     *
     * @param context The context to use. Usually your Activity object.
     */
    public static void loadFromContext(Context context) {
        loadFilesPath(context);
    }

    /**
     * Helper method to set the files path. If an exception occurs, the files
     * path will be null!
     *
     * @param context The context to use. Usually your Activity object.
     */
    private static void loadFilesPath(Context context) {
        if (context != null) {
            try {
                File file = context.getFilesDir();

                // The file shouldn't be null, but apparently it still can happen, see
                // http://code.google.com/p/android/issues/detail?id=8886
                if (file != null) {
                    Constants.FILES_PATH = file.getAbsolutePath();
                }
            } catch (Exception e) {
                AvalancheLog.error("Exception thrown when accessing the files dir:");
                e.printStackTrace();
            }
        }
    }
}
