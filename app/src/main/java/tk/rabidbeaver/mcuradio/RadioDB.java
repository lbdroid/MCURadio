package tk.rabidbeaver.mcuradio;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class RadioDB extends SQLiteOpenHelper {
	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "RBRadio";
	
	public RadioDB(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		String radiocreate = "CREATE TABLE radiosettings (id INTEGER PRIMARY KEY AUTOINCREMENT, lastfm INTEGER, lastam INTEGER, lastbandfm INTEGER)";
		String favscreate = "CREATE TABLE radiofavorites(id INTEGER PRIMARY KEY AUTOINCREMENT, priority INTEGER, freq INTEGER, fm INTEGER)";
		
		db.execSQL(radiocreate);
		db.execSQL(favscreate);
		
		String setdefaults = "INSERT INTO radiosettings (lastfm, lastam, lastbandfm) VALUES (9310, 1010, 0)";
		db.execSQL(setdefaults);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// nothing to do until the next version....
	}
	
	public int getLastFM(){
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery("SELECT lastfm FROM radiosettings LIMIT 1", null);
		if (cursor.moveToFirst()) {
			return cursor.getInt(0);
		}
		return 9310;
	}

	public void setLastFreq(int lastam, int lastfm, boolean fm){
		Log.d("RADIODB", "LASTAM: "+lastam+", LASTFM: "+lastfm+", FM: "+fm);
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("lastfm", lastfm);
		values.put("lastam", lastam);
		values.put("lastbandfm", fm?1:0);
		db.update("radiosettings", values, "id = ?", new String[]{"1"});
		db.close();
	}

	public int getLastAM(){
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery("SELECT lastam FROM radiosettings LIMIT 1", null);
		if (cursor.moveToFirst()) {
			return cursor.getInt(0);
		}
		return 1010;
	}

	public boolean isLastBandFM(){
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery("SELECT lastbandfm FROM radiosettings LIMIT 1", null);
		if (cursor.moveToFirst()) {
			return cursor.getInt(0)==1;
		}
		return false;
	}
	
	private void resyncFavs(){
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery("SELECT freq, fm FROM radiofavorites ORDER BY priority ASC", null);
		int rows = cursor.getCount();
		Channel[] oldorder = new Channel[rows];

		for (int i=0; i<rows; i++){
			cursor.moveToPosition(i);
			oldorder[i] = new Channel(cursor.getInt(0), cursor.getInt(1)==1, -1);
		}
		
		for (int i=rows-1; i>=0; i--){
			db.execSQL("UPDATE radiofavorites SET priority="+10*(i+1)+" WHERE freq='"+oldorder[i].frequency+"' AND fm='"+oldorder[i].fm+"'");
		}
		
		db.close();
	}
	
	public void moveFav(Channel channel, boolean up){
		SQLiteDatabase db = this.getWritableDatabase();

		// first, find the channel's old priority.
		Cursor cursor = db.rawQuery("SELECT priority FROM radiofavorites WHERE freq='"+channel.frequency+"' AND fm='"+channel.fm+"'",null);
		int priority = 0;
		if (cursor.moveToFirst()){
			priority = cursor.getInt(0);
		} else return;
		
		// if we are increasing priority, but already are maximum, do nothing -- return.
		if (!up && priority == 10) return;
		
		// increment the priority past the adjacent favorite
		priority = up?priority+11:priority-11;
		db.execSQL("UPDATE radiofavorites SET priority ="+priority+" WHERE freq='"+channel.frequency+"' AND fm='"+channel.fm+"'");
		
		db.close();
		resyncFavs();
	}
	
	public void setFav(Channel channel, boolean add){
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete("radiofavorites", "freq = ? AND fm = ?", new String[]{Integer.toString(channel.frequency), channel.fm?"1":"0"});
		if (add){
			Cursor cursor = db.rawQuery("SELECT MAX(priority) FROM radiofavorites", null);
			int maxpriority = 0;
			if (cursor.moveToFirst()){
				maxpriority = cursor.getInt(0);
			}
			ContentValues values = new ContentValues();
			values.put("freq", channel.frequency);
			values.put("priority", maxpriority+10);
			values.put("fm", channel.fm?1:0);
			db.insert("radiofavorites", null, values);
		}
		db.close();
		resyncFavs();
	}
	
	public boolean isFavorite(Channel c){
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery("SELECT * FROM radiofavorites WHERE freq='"+c.frequency+"' AND fm='"+(c.fm?"1":"0")+"'", null);
		if (cursor.moveToFirst()) {
			db.close();
			return true;
		}
		db.close();
		return false;
	}
	
	public Channel[] getAllFavorites(){
		Channel[] retval = null;
		int rows = 0;
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery("SELECT freq, fm, priority FROM radiofavorites ORDER BY priority ASC", null);
		rows = cursor.getCount();
		retval = new Channel[rows];
		for (int i=0; i<rows; i++){
			cursor.moveToPosition(i);
			retval[i] = new Channel(cursor.getInt(0), cursor.getInt(1)==1, cursor.getInt(2));
		}
		return retval;
	}
}
