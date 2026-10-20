package fr.gambitdrame;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Local, user-exportable Stockfish diagnostics. No network; stores a bounded
 * journal inside the app so crashes/restarts do not discard all clues.
 */
public final class EngineDiagnostics {
    private static final int MAX_CHARS = 65000;
    private static final String TAG = "GambitEngine";
    private final SharedPreferences prefs;
    private final StringBuilder entries;
    public EngineDiagnostics(Context context) {
        prefs=context.getApplicationContext().getSharedPreferences("gambit-engine-diagnostics",Context.MODE_PRIVATE);
        entries=new StringBuilder(prefs.getString("journal",""));
    }
    public synchronized void record(String category,String message) {
        String stamp=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS",Locale.FRANCE).format(new Date());
        String line=stamp+" ["+category+"] "+message.replace('\n',' ')+"\n";
        entries.append(line);
        if(entries.length()>MAX_CHARS) {
            int threshold=entries.length()-MAX_CHARS;
            int lineEnd=entries.indexOf("\n",threshold);
            entries.delete(0,lineEnd>=0?lineEnd+1:threshold);
        }
        // Keep the journal if Android terminates the process.
        prefs.edit().putString("journal",entries.toString()).apply();
        Log.i(TAG,line.trim());
    }
    public void error(String category,Throwable ex) {
        record("ERR:"+category,String.valueOf(ex));
        record("ERR_STACK:"+category,Log.getStackTraceString(ex));
        Log.e(TAG,category,ex);
    }
    public synchronized String text(){return entries.toString();}
    public synchronized void clear() {
        entries.setLength(0);
        prefs.edit().remove("journal").apply();
        Log.i(TAG,"Journal cleared by user");
    }
}
