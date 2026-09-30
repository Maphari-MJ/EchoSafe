package com.echosafe.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class EvidenceDao_Impl implements EvidenceDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<PendingEvidenceUploadEntity> __insertionAdapterOfPendingEvidenceUploadEntity;

  private final SharedSQLiteStatement __preparedStmtOfMarkUploaded;

  public EvidenceDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPendingEvidenceUploadEntity = new EntityInsertionAdapter<PendingEvidenceUploadEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `pending_evidence_uploads` (`evidenceId`,`localFilePath`,`mediaType`,`sha256Hash`,`timestamp`,`isUploaded`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PendingEvidenceUploadEntity entity) {
        statement.bindString(1, entity.getEvidenceId());
        statement.bindString(2, entity.getLocalFilePath());
        statement.bindString(3, entity.getMediaType());
        statement.bindString(4, entity.getSha256Hash());
        statement.bindLong(5, entity.getTimestamp());
        final int _tmp = entity.isUploaded() ? 1 : 0;
        statement.bindLong(6, _tmp);
      }
    };
    this.__preparedStmtOfMarkUploaded = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE pending_evidence_uploads SET isUploaded = 1 WHERE evidenceId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertEvidence(final PendingEvidenceUploadEntity evidence,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPendingEvidenceUploadEntity.insert(evidence);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object markUploaded(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkUploaded.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfMarkUploaded.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getPendingUploads(
      final Continuation<? super List<PendingEvidenceUploadEntity>> $completion) {
    final String _sql = "SELECT * FROM pending_evidence_uploads WHERE isUploaded = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<PendingEvidenceUploadEntity>>() {
      @Override
      @NonNull
      public List<PendingEvidenceUploadEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfEvidenceId = CursorUtil.getColumnIndexOrThrow(_cursor, "evidenceId");
          final int _cursorIndexOfLocalFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "localFilePath");
          final int _cursorIndexOfMediaType = CursorUtil.getColumnIndexOrThrow(_cursor, "mediaType");
          final int _cursorIndexOfSha256Hash = CursorUtil.getColumnIndexOrThrow(_cursor, "sha256Hash");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfIsUploaded = CursorUtil.getColumnIndexOrThrow(_cursor, "isUploaded");
          final List<PendingEvidenceUploadEntity> _result = new ArrayList<PendingEvidenceUploadEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PendingEvidenceUploadEntity _item;
            final String _tmpEvidenceId;
            _tmpEvidenceId = _cursor.getString(_cursorIndexOfEvidenceId);
            final String _tmpLocalFilePath;
            _tmpLocalFilePath = _cursor.getString(_cursorIndexOfLocalFilePath);
            final String _tmpMediaType;
            _tmpMediaType = _cursor.getString(_cursorIndexOfMediaType);
            final String _tmpSha256Hash;
            _tmpSha256Hash = _cursor.getString(_cursorIndexOfSha256Hash);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final boolean _tmpIsUploaded;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsUploaded);
            _tmpIsUploaded = _tmp != 0;
            _item = new PendingEvidenceUploadEntity(_tmpEvidenceId,_tmpLocalFilePath,_tmpMediaType,_tmpSha256Hash,_tmpTimestamp,_tmpIsUploaded);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getAllEvidence(
      final Continuation<? super List<PendingEvidenceUploadEntity>> $completion) {
    final String _sql = "SELECT * FROM pending_evidence_uploads ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<PendingEvidenceUploadEntity>>() {
      @Override
      @NonNull
      public List<PendingEvidenceUploadEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfEvidenceId = CursorUtil.getColumnIndexOrThrow(_cursor, "evidenceId");
          final int _cursorIndexOfLocalFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "localFilePath");
          final int _cursorIndexOfMediaType = CursorUtil.getColumnIndexOrThrow(_cursor, "mediaType");
          final int _cursorIndexOfSha256Hash = CursorUtil.getColumnIndexOrThrow(_cursor, "sha256Hash");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfIsUploaded = CursorUtil.getColumnIndexOrThrow(_cursor, "isUploaded");
          final List<PendingEvidenceUploadEntity> _result = new ArrayList<PendingEvidenceUploadEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PendingEvidenceUploadEntity _item;
            final String _tmpEvidenceId;
            _tmpEvidenceId = _cursor.getString(_cursorIndexOfEvidenceId);
            final String _tmpLocalFilePath;
            _tmpLocalFilePath = _cursor.getString(_cursorIndexOfLocalFilePath);
            final String _tmpMediaType;
            _tmpMediaType = _cursor.getString(_cursorIndexOfMediaType);
            final String _tmpSha256Hash;
            _tmpSha256Hash = _cursor.getString(_cursorIndexOfSha256Hash);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final boolean _tmpIsUploaded;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsUploaded);
            _tmpIsUploaded = _tmp != 0;
            _item = new PendingEvidenceUploadEntity(_tmpEvidenceId,_tmpLocalFilePath,_tmpMediaType,_tmpSha256Hash,_tmpTimestamp,_tmpIsUploaded);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
