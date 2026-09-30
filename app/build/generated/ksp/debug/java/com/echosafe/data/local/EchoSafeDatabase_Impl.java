package com.echosafe.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class EchoSafeDatabase_Impl extends EchoSafeDatabase {
  private volatile TriggerDao _triggerDao;

  private volatile CalibrationDao _calibrationDao;

  private volatile EvidenceDao _evidenceDao;

  private volatile ContactDao _contactDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `offline_triggers` (`triggerId` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `triggerType` TEXT NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `dispatchChannel` TEXT NOT NULL, `isSynced` INTEGER NOT NULL, PRIMARY KEY(`triggerId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `voice_calibration_profile` (`id` INTEGER NOT NULL, `normalPitchBaseline` REAL NOT NULL, `screamPitchThreshold` REAL NOT NULL, `secretKeyphraseHash` TEXT NOT NULL, `safeWordHash` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `pending_evidence_uploads` (`evidenceId` TEXT NOT NULL, `localFilePath` TEXT NOT NULL, `mediaType` TEXT NOT NULL, `sha256Hash` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `isUploaded` INTEGER NOT NULL, PRIMARY KEY(`evidenceId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `emergency_contacts` (`contactId` TEXT NOT NULL, `contactName` TEXT NOT NULL, `phoneNumber` TEXT NOT NULL, `relationship` TEXT NOT NULL, `fcmToken` TEXT NOT NULL, `hasDndPermission` INTEGER NOT NULL, PRIMARY KEY(`contactId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'fb018fc98444b9c5a74c4df9d200f581')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `offline_triggers`");
        db.execSQL("DROP TABLE IF EXISTS `voice_calibration_profile`");
        db.execSQL("DROP TABLE IF EXISTS `pending_evidence_uploads`");
        db.execSQL("DROP TABLE IF EXISTS `emergency_contacts`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsOfflineTriggers = new HashMap<String, TableInfo.Column>(7);
        _columnsOfflineTriggers.put("triggerId", new TableInfo.Column("triggerId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOfflineTriggers.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOfflineTriggers.put("triggerType", new TableInfo.Column("triggerType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOfflineTriggers.put("latitude", new TableInfo.Column("latitude", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOfflineTriggers.put("longitude", new TableInfo.Column("longitude", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOfflineTriggers.put("dispatchChannel", new TableInfo.Column("dispatchChannel", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOfflineTriggers.put("isSynced", new TableInfo.Column("isSynced", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysOfflineTriggers = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesOfflineTriggers = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoOfflineTriggers = new TableInfo("offline_triggers", _columnsOfflineTriggers, _foreignKeysOfflineTriggers, _indicesOfflineTriggers);
        final TableInfo _existingOfflineTriggers = TableInfo.read(db, "offline_triggers");
        if (!_infoOfflineTriggers.equals(_existingOfflineTriggers)) {
          return new RoomOpenHelper.ValidationResult(false, "offline_triggers(com.echosafe.data.local.OfflineTriggerEntity).\n"
                  + " Expected:\n" + _infoOfflineTriggers + "\n"
                  + " Found:\n" + _existingOfflineTriggers);
        }
        final HashMap<String, TableInfo.Column> _columnsVoiceCalibrationProfile = new HashMap<String, TableInfo.Column>(5);
        _columnsVoiceCalibrationProfile.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVoiceCalibrationProfile.put("normalPitchBaseline", new TableInfo.Column("normalPitchBaseline", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVoiceCalibrationProfile.put("screamPitchThreshold", new TableInfo.Column("screamPitchThreshold", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVoiceCalibrationProfile.put("secretKeyphraseHash", new TableInfo.Column("secretKeyphraseHash", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVoiceCalibrationProfile.put("safeWordHash", new TableInfo.Column("safeWordHash", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysVoiceCalibrationProfile = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesVoiceCalibrationProfile = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoVoiceCalibrationProfile = new TableInfo("voice_calibration_profile", _columnsVoiceCalibrationProfile, _foreignKeysVoiceCalibrationProfile, _indicesVoiceCalibrationProfile);
        final TableInfo _existingVoiceCalibrationProfile = TableInfo.read(db, "voice_calibration_profile");
        if (!_infoVoiceCalibrationProfile.equals(_existingVoiceCalibrationProfile)) {
          return new RoomOpenHelper.ValidationResult(false, "voice_calibration_profile(com.echosafe.data.local.VoiceCalibrationProfileEntity).\n"
                  + " Expected:\n" + _infoVoiceCalibrationProfile + "\n"
                  + " Found:\n" + _existingVoiceCalibrationProfile);
        }
        final HashMap<String, TableInfo.Column> _columnsPendingEvidenceUploads = new HashMap<String, TableInfo.Column>(6);
        _columnsPendingEvidenceUploads.put("evidenceId", new TableInfo.Column("evidenceId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingEvidenceUploads.put("localFilePath", new TableInfo.Column("localFilePath", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingEvidenceUploads.put("mediaType", new TableInfo.Column("mediaType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingEvidenceUploads.put("sha256Hash", new TableInfo.Column("sha256Hash", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingEvidenceUploads.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingEvidenceUploads.put("isUploaded", new TableInfo.Column("isUploaded", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPendingEvidenceUploads = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPendingEvidenceUploads = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPendingEvidenceUploads = new TableInfo("pending_evidence_uploads", _columnsPendingEvidenceUploads, _foreignKeysPendingEvidenceUploads, _indicesPendingEvidenceUploads);
        final TableInfo _existingPendingEvidenceUploads = TableInfo.read(db, "pending_evidence_uploads");
        if (!_infoPendingEvidenceUploads.equals(_existingPendingEvidenceUploads)) {
          return new RoomOpenHelper.ValidationResult(false, "pending_evidence_uploads(com.echosafe.data.local.PendingEvidenceUploadEntity).\n"
                  + " Expected:\n" + _infoPendingEvidenceUploads + "\n"
                  + " Found:\n" + _existingPendingEvidenceUploads);
        }
        final HashMap<String, TableInfo.Column> _columnsEmergencyContacts = new HashMap<String, TableInfo.Column>(6);
        _columnsEmergencyContacts.put("contactId", new TableInfo.Column("contactId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEmergencyContacts.put("contactName", new TableInfo.Column("contactName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEmergencyContacts.put("phoneNumber", new TableInfo.Column("phoneNumber", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEmergencyContacts.put("relationship", new TableInfo.Column("relationship", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEmergencyContacts.put("fcmToken", new TableInfo.Column("fcmToken", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEmergencyContacts.put("hasDndPermission", new TableInfo.Column("hasDndPermission", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysEmergencyContacts = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesEmergencyContacts = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoEmergencyContacts = new TableInfo("emergency_contacts", _columnsEmergencyContacts, _foreignKeysEmergencyContacts, _indicesEmergencyContacts);
        final TableInfo _existingEmergencyContacts = TableInfo.read(db, "emergency_contacts");
        if (!_infoEmergencyContacts.equals(_existingEmergencyContacts)) {
          return new RoomOpenHelper.ValidationResult(false, "emergency_contacts(com.echosafe.data.local.ContactEntity).\n"
                  + " Expected:\n" + _infoEmergencyContacts + "\n"
                  + " Found:\n" + _existingEmergencyContacts);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "fb018fc98444b9c5a74c4df9d200f581", "b0d421060f7d9919d7a424199fff3338");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "offline_triggers","voice_calibration_profile","pending_evidence_uploads","emergency_contacts");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `offline_triggers`");
      _db.execSQL("DELETE FROM `voice_calibration_profile`");
      _db.execSQL("DELETE FROM `pending_evidence_uploads`");
      _db.execSQL("DELETE FROM `emergency_contacts`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(TriggerDao.class, TriggerDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CalibrationDao.class, CalibrationDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(EvidenceDao.class, EvidenceDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ContactDao.class, ContactDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public TriggerDao triggerDao() {
    if (_triggerDao != null) {
      return _triggerDao;
    } else {
      synchronized(this) {
        if(_triggerDao == null) {
          _triggerDao = new TriggerDao_Impl(this);
        }
        return _triggerDao;
      }
    }
  }

  @Override
  public CalibrationDao calibrationDao() {
    if (_calibrationDao != null) {
      return _calibrationDao;
    } else {
      synchronized(this) {
        if(_calibrationDao == null) {
          _calibrationDao = new CalibrationDao_Impl(this);
        }
        return _calibrationDao;
      }
    }
  }

  @Override
  public EvidenceDao evidenceDao() {
    if (_evidenceDao != null) {
      return _evidenceDao;
    } else {
      synchronized(this) {
        if(_evidenceDao == null) {
          _evidenceDao = new EvidenceDao_Impl(this);
        }
        return _evidenceDao;
      }
    }
  }

  @Override
  public ContactDao contactDao() {
    if (_contactDao != null) {
      return _contactDao;
    } else {
      synchronized(this) {
        if(_contactDao == null) {
          _contactDao = new ContactDao_Impl(this);
        }
        return _contactDao;
      }
    }
  }
}
