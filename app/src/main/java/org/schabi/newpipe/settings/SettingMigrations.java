package org.schabi.newpipe.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import org.schabi.newpipe.R;
import org.schabi.newpipe.report.ErrorActivity;
import org.schabi.newpipe.report.ErrorActivity.ErrorInfo;
import org.schabi.newpipe.report.UserAction;

public final class SettingMigrations {
    /**
     * Version number for preferences. Must be incremented every time a migration is necessary.
     */
    public static final int VERSION = 1;
    private static SharedPreferences sp;

    public static final Migration MIGRATION_0_1 = new Migration(0, 1) {
        @Override
        public void migrate(final Context context) {
            // We changed the content of the dialog which opens when sharing a link to NewPipe
            // by removing the "open detail page" option.
            // Therefore, show the dialog once again to ensure users need to choose again and are
            // aware of the changed dialog.
            final SharedPreferences.Editor editor = sp.edit();
            editor.putString(context.getString(R.string.preferred_open_action_key),
                    context.getString(R.string.always_ask_open_action_key));
            editor.apply();
        }
    };

    /**
     * List of all implemented migrations.
     * <p>
     * <b>Append new migrations to the end of the list</b> to keep it sorted ascending.
     * If not sorted correctly, migrations which depend on each other, may fail.
     */
    private static final Migration[] SETTING_MIGRATIONS = {
        MIGRATION_0_1
    };


    public static void initMigrations(final Context context, final boolean isFirstRun) {
        // setup migrations and check if there is something to do
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        final String lastPrefVersionKey = context.getString(R.string.last_used_preferences_version);
        final int lastPrefVersion = sp.getInt(lastPrefVersionKey, 0);

        // no migration to run, already up to date
        if (isFirstRun) {
            sp.edit().putInt(lastPrefVersionKey, VERSION).apply();
            return;
        } else if (lastPrefVersion == VERSION) {
            return;
        }

        // run migrations
        int currentVersion = lastPrefVersion;
        for (final Migration currentMigration : SETTING_MIGRATIONS) {
            try {
                if (currentMigration.shouldMigrate()) {
                    currentMigration.migrate(context);
                    currentVersion = currentMigration.newVersion;
                }
            } catch (final Exception e) {
                // save the version with the last successful migration and report the error
                sp.edit().putInt(lastPrefVersionKey, currentVersion).apply();
                final ErrorInfo errorInfo = ErrorInfo.make(
                        UserAction.PREFERENCES_MIGRATION,
                        "none",
                        "Migrating preferences from version " + lastPrefVersion + " to "
                                + VERSION + ". "
                                + "Error at " + currentVersion  + " => " + ++currentVersion,
                        0
                );
                ErrorActivity.reportError(context, e, SettingMigrations.class, null, errorInfo);
                return;
            }
        }

        // store the current preferences version
        sp.edit().putInt(lastPrefVersionKey, currentVersion).apply();
    }

    private SettingMigrations() { }

    abstract static class Migration {
        public final int oldVersion;
        public final int newVersion;

        protected Migration(final int oldVersion, final int newVersion) {
            this.oldVersion = oldVersion;
            this.newVersion = newVersion;
        }

        /**
         * @return Returns whether this migration should be run.
         * because the last run of NewPipe happened with an older preferences version
         * than the version this migration is updating to.
         */
        protected final boolean shouldMigrate() {
            return oldVersion < VERSION;
        }

        protected abstract void migrate(Context context);

    }

}
