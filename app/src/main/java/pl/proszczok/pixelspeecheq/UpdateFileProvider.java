package pl.proszczok.pixelspeecheq;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

public final class UpdateFileProvider extends ContentProvider {
    static final String FILE_NAME = "update.apk";

    static File updateFile(android.content.Context context) {
        return new File(new File(context.getCacheDir(), "updates"), FILE_NAME);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public String getType(Uri uri) {
        requireUpdateUri(uri);
        return "application/vnd.android.package-archive";
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        requireUpdateUri(uri);
        File file = updateFile(getContext());
        String[] requested = projection == null
                ? new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}
                : projection;
        MatrixCursor cursor = new MatrixCursor(requested, 1);
        MatrixCursor.RowBuilder row = cursor.newRow();
        for (String column : requested) {
            if (OpenableColumns.DISPLAY_NAME.equals(column)) {
                row.add(FILE_NAME);
            } else if (OpenableColumns.SIZE.equals(column)) {
                row.add(file.length());
            } else {
                row.add(null);
            }
        }
        return cursor;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode)
            throws FileNotFoundException {
        requireUpdateUri(uri);
        if (!"r".equals(mode)) throw new FileNotFoundException("Tylko odczyt");
        File file = updateFile(getContext());
        if (!file.isFile()) throw new FileNotFoundException("Brak pobranej aktualizacji");
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Tylko odczyt");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Tylko odczyt");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        throw new UnsupportedOperationException("Tylko odczyt");
    }

    private static void requireUpdateUri(Uri uri) {
        if (uri == null || uri.getPathSegments().size() != 1
                || !FILE_NAME.equals(uri.getLastPathSegment())) {
            throw new IllegalArgumentException("Nieprawidłowy plik aktualizacji");
        }
    }
}
