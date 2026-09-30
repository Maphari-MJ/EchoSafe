package com.echosafe.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class CalibrationDao_Impl implements CalibrationDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<VoiceCalibrationProfileEntity> __insertionAdapterOfVoiceCalibrationProfileEntity;

  public CalibrationDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfVoiceCalibrationProfileEntity = new EntityInsertionAdapter<VoiceCalibrationProfileEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `voice_calibration_profile` (`id`,`normalPitchBaseline`,`screamPitchThreshold`,`secretKeyphraseHash`,`safeWordHash`) VALUES (?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final VoiceCalibrationProfileEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindDouble(2, entity.getNormalPitchBaseline());
        statement.bindDouble(3, entity.getScreamPitchThreshold());
        statement.bindString(4, entity.getSecretKeyphraseHash());
        statement.bindString(5, entity.getSafeWordHash());
      }
    };
  }

  @Override
  public Object saveProfile(final VoiceCalibrationProfileEntity profile,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfVoiceCalibrationProfileEntity.insert(profile);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object getProfile(final Continuation<? super VoiceCalibrationProfileEntity> $completion) {
    final String _sql = "SELECT * FROM voice_calibration_profile WHERE id = 1 LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<VoiceCalibrationProfileEntity>() {
      @Override
      @Nullable
      public VoiceCalibrationProfileEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfNormalPitchBaseline = CursorUtil.getColumnIndexOrThrow(_cursor, "normalPitchBaseline");
          final int _cursorIndexOfScreamPitchThreshold = CursorUtil.getColumnIndexOrThrow(_cursor, "screamPitchThreshold");
          final int _cursorIndexOfSecretKeyphraseHash = CursorUtil.getColumnIndexOrThrow(_cursor, "secretKeyphraseHash");
          final int _cursorIndexOfSafeWordHash = CursorUtil.getColumnIndexOrThrow(_cursor, "safeWordHash");
          final VoiceCalibrationProfileEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final double _tmpNormalPitchBaseline;
            _tmpNormalPitchBaseline = _cursor.getDouble(_cursorIndexOfNormalPitchBaseline);
            final double _tmpScreamPitchThreshold;
            _tmpScreamPitchThreshold = _cursor.getDouble(_cursorIndexOfScreamPitchThreshold);
            final String _tmpSecretKeyphraseHash;
            _tmpSecretKeyphraseHash = _cursor.getString(_cursorIndexOfSecretKeyphraseHash);
            final String _tmpSafeWordHash;
            _tmpSafeWordHash = _cursor.getString(_cursorIndexOfSafeWordHash);
            _result = new VoiceCalibrationProfileEntity(_tmpId,_tmpNormalPitchBaseline,_tmpScreamPitchThreshold,_tmpSecretKeyphraseHash,_tmpSafeWordHash);
          } else {
            _result = null;
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
