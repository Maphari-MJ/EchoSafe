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
public final class TriggerDao_Impl implements TriggerDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<OfflineTriggerEntity> __insertionAdapterOfOfflineTriggerEntity;

  private final SharedSQLiteStatement __preparedStmtOfMarkSynced;

  public TriggerDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfOfflineTriggerEntity = new EntityInsertionAdapter<OfflineTriggerEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `offline_triggers` (`triggerId`,`timestamp`,`triggerType`,`latitude`,`longitude`,`dispatchChannel`,`isSynced`) VALUES (?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final OfflineTriggerEntity entity) {
        statement.bindString(1, entity.getTriggerId());
        statement.bindLong(2, entity.getTimestamp());
        statement.bindString(3, entity.getTriggerType());
        statement.bindDouble(4, entity.getLatitude());
        statement.bindDouble(5, entity.getLongitude());
        statement.bindString(6, entity.getDispatchChannel());
        final int _tmp = entity.isSynced() ? 1 : 0;
        statement.bindLong(7, _tmp);
      }
    };
    this.__preparedStmtOfMarkSynced = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE offline_triggers SET isSynced = 1 WHERE triggerId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertTrigger(final OfflineTriggerEntity trigger,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfOfflineTriggerEntity.insert(trigger);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object markSynced(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkSynced.acquire();
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
          __preparedStmtOfMarkSynced.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getUnsyncedTriggers(
      final Continuation<? super List<OfflineTriggerEntity>> $completion) {
    final String _sql = "SELECT * FROM offline_triggers WHERE isSynced = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<OfflineTriggerEntity>>() {
      @Override
      @NonNull
      public List<OfflineTriggerEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfTriggerId = CursorUtil.getColumnIndexOrThrow(_cursor, "triggerId");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfTriggerType = CursorUtil.getColumnIndexOrThrow(_cursor, "triggerType");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfDispatchChannel = CursorUtil.getColumnIndexOrThrow(_cursor, "dispatchChannel");
          final int _cursorIndexOfIsSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "isSynced");
          final List<OfflineTriggerEntity> _result = new ArrayList<OfflineTriggerEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final OfflineTriggerEntity _item;
            final String _tmpTriggerId;
            _tmpTriggerId = _cursor.getString(_cursorIndexOfTriggerId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpTriggerType;
            _tmpTriggerType = _cursor.getString(_cursorIndexOfTriggerType);
            final double _tmpLatitude;
            _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            final double _tmpLongitude;
            _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            final String _tmpDispatchChannel;
            _tmpDispatchChannel = _cursor.getString(_cursorIndexOfDispatchChannel);
            final boolean _tmpIsSynced;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsSynced);
            _tmpIsSynced = _tmp != 0;
            _item = new OfflineTriggerEntity(_tmpTriggerId,_tmpTimestamp,_tmpTriggerType,_tmpLatitude,_tmpLongitude,_tmpDispatchChannel,_tmpIsSynced);
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
  public Object getAllTriggers(final Continuation<? super List<OfflineTriggerEntity>> $completion) {
    final String _sql = "SELECT * FROM offline_triggers ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<OfflineTriggerEntity>>() {
      @Override
      @NonNull
      public List<OfflineTriggerEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfTriggerId = CursorUtil.getColumnIndexOrThrow(_cursor, "triggerId");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfTriggerType = CursorUtil.getColumnIndexOrThrow(_cursor, "triggerType");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfDispatchChannel = CursorUtil.getColumnIndexOrThrow(_cursor, "dispatchChannel");
          final int _cursorIndexOfIsSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "isSynced");
          final List<OfflineTriggerEntity> _result = new ArrayList<OfflineTriggerEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final OfflineTriggerEntity _item;
            final String _tmpTriggerId;
            _tmpTriggerId = _cursor.getString(_cursorIndexOfTriggerId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpTriggerType;
            _tmpTriggerType = _cursor.getString(_cursorIndexOfTriggerType);
            final double _tmpLatitude;
            _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            final double _tmpLongitude;
            _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            final String _tmpDispatchChannel;
            _tmpDispatchChannel = _cursor.getString(_cursorIndexOfDispatchChannel);
            final boolean _tmpIsSynced;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsSynced);
            _tmpIsSynced = _tmp != 0;
            _item = new OfflineTriggerEntity(_tmpTriggerId,_tmpTimestamp,_tmpTriggerType,_tmpLatitude,_tmpLongitude,_tmpDispatchChannel,_tmpIsSynced);
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
