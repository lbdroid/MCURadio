package tk.rabidbeaver.mcuradio;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

public class Radio extends Activity {

	private RadioDB rdb;
	private Button channelbtn;
	private ImageButton togglefav;
	//TODO private TextView radiotext;
	//TODO private TextView programservice;

	protected static int lastFreqAM = 1010;
	protected static int lastFreqFM = 9310;

	private Channel[] favs;
	private boolean lastChannelIsFav = false;

	int freq = 1010;

	private LinearLayout favorites;
	
	protected static boolean band_fm = false;

	private Button amfmbtn;
	
	private EditText channelinput;
	private Button dialogband;

	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			//int band = intent.getIntExtra("BAND", -1);
			//int channel = intent.getIntExtra("CHANNEL", -1);
			//int area = intent.getIntExtra("AREA", -1);
			int freq = intent.getIntExtra("FREQ", -1);
			//int ptyid = intent.getIntExtra("PTYID", -1);
			//String rdstext = intent.getStringExtra("RDSTEXT");
			//String pstext = intent.getStringExtra("PSTEXT");
			//int loc = intent.getIntExtra("LOC", -1);
			//int stereo = intent.getIntExtra("STEREO", -1);
			if (freq > 0){
				String value = (band_fm?Float.toString(((float)freq)/100):Integer.toString(freq)) + (band_fm?" MHz":" KHz");
				channelbtn.setText(value);
			}
		}
	};

	private void tune(int frequency, boolean fm){
		Log.d("RADIOTUNE", "FREQ: "+frequency+", FM: "+fm);
		freq = frequency;
		band_fm = fm;
		Intent i = new Intent();
		i.setAction("tk.rabidbeaver.radiocontroller.BAND");
		i.putExtra("BAND", band_fm?"FM":"AM");
		sendBroadcast(i);

		i = new Intent();
		i.setAction("tk.rabidbeaver.radiocontroller.TUNE");
		i.putExtra("FREQ", frequency);
		sendBroadcast(i);

		if (band_fm) lastFreqFM = frequency;
		else lastFreqAM = frequency;

		rdb.setLastFreq(lastFreqAM, lastFreqFM, fm);

		if (rdb.isFavorite(new Channel(frequency, fm))){
			lastChannelIsFav = true;
			togglefav.setImageResource(R.drawable.ic_star_selected);
		} else {
			lastChannelIsFav = false;
			togglefav.setImageResource(R.drawable.ic_star);
		}

		amfmbtn.setText(band_fm?"FM":"AM");
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_radio);
		rdb = new RadioDB(this);
		lastFreqAM = rdb.getLastAM();
		Log.d("RADIODB", "LASTFM:"+lastFreqFM);
		lastFreqFM = rdb.getLastFM();
		Log.d("RADIODB", "LASTFM:"+lastFreqFM);
		band_fm = rdb.isLastBandFM();
		if (band_fm) freq = lastFreqFM;
		else freq = lastFreqAM;

		IntentFilter filter = new IntentFilter();
		filter.addAction("tk.rabidbeaver.radiocontroller.BROADCAST");
		registerReceiver(receiver, filter);

		//channel = (Button) this.findViewById(R.id.channelline);
		//TODO radiotext = findViewById(R.id.radiotextline);
		//TODO programservice = findViewById(R.id.programserviceline);
		
		channelbtn = findViewById(R.id.channelline);
		channelbtn.setOnClickListener(new Button.OnClickListener(){
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(Radio.this);
				builder.setTitle("Enter New Channel:");
				builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			        	   //TODO: add some format checking....

						   tune(Integer.parseInt(channelinput.getText().toString()), dialogband.getText().toString().contentEquals("FM"));

			        	   dialog.dismiss();
			           }
			       });
				builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			        	   dialog.dismiss();
			           }
			       });

				AlertDialog dialog = builder.create();
				
				LayoutInflater inflater=Radio.this.getLayoutInflater();
				View layout=inflater.inflate(R.layout.dialog_channel,(ViewGroup)getWindow().getDecorView().findViewById(R.layout.activity_radio));
				dialog.setView(layout);
				channelinput = layout.findViewById(R.id.channel);
				dialogband = layout.findViewById(R.id.dialog_amfm);
				//channelinput.append(lastChannel.split(" ")[0]);
				dialogband.setText(band_fm?"FM":"AM");
				dialogband.setOnClickListener(new Button.OnClickListener(){
					@Override
					public void onClick(View v) {
						String am = "AM";
						String fm = "FM";
						if (dialogband.getText().toString().equalsIgnoreCase("AM")) dialogband.setText(fm);
						else dialogband.setText(am);
					}
				});
				dialog.show();
			}
		});

		String amfm = Integer.toString(freq) + (band_fm?" FM":" AM");
		channelbtn.setText(amfm);
		
		amfmbtn = findViewById(R.id.amfmbtn);
		amfmbtn.setOnClickListener(new Button.OnClickListener(){
			@Override
			public void onClick(View v) {
				if (band_fm){
					tune(lastFreqAM, !band_fm);
				} else {
					tune(lastFreqFM, !band_fm);
				}
			}
		});
		
		togglefav = findViewById(R.id.favorite);
		togglefav.setOnClickListener(new ImageButton.OnClickListener(){
			@Override
			public void onClick(View v) {
				if (lastChannelIsFav){
					((ImageButton) v).setImageResource(R.drawable.ic_star);
					lastChannelIsFav = false;
					rdb.setFav(new Channel(freq, band_fm), false);
				} else {
					((ImageButton) v).setImageResource(R.drawable.ic_star_selected);
					lastChannelIsFav = true;
					rdb.setFav(new Channel(freq, band_fm), true);
				}
				refreshfav();
			}
		});
		
		Button seekupbtn = findViewById(R.id.seekup);
		seekupbtn.setOnClickListener(new Button.OnClickListener(){
			@Override
			public void onClick(View v) {
				Intent i = new Intent();
				i.setAction("tk.rabidbeaver.radiocontroller.SEEK_UP");
				sendBroadcast(i);
			}
		});
		
		Button seekdownbtn = findViewById(R.id.seekdown);
		seekdownbtn.setOnClickListener(new Button.OnClickListener(){
			@Override
			public void onClick(View v) {
				Intent i = new Intent();
				i.setAction("tk.rabidbeaver.radiocontroller.SEEK_DOWN");
				sendBroadcast(i);
			}
		});
		
		Button tuneupbtn = findViewById(R.id.tuneup);
		tuneupbtn.setOnClickListener(new Button.OnClickListener(){
			@Override
			public void onClick(View v) {
				if (band_fm) {
					lastFreqFM += 10;
					tune(lastFreqFM, band_fm);
				} else {
					lastFreqAM += 10;
					tune(lastFreqAM, band_fm);
				}
			}
		});
		
		Button tunedownbtn = findViewById(R.id.tunedown);
		tunedownbtn.setOnClickListener(new Button.OnClickListener(){
			@Override
			public void onClick(View v) {
				if (band_fm) {
					lastFreqFM -= 10;
					tune(lastFreqFM, band_fm);
				} else {
					lastFreqAM -= 10;
					tune(lastFreqAM, band_fm);
				}
			}
		});
		
		favorites = findViewById(R.id.favorites);
		refreshfav();
		tune(band_fm?lastFreqFM:lastFreqAM, band_fm);
	}
	
	public void refreshfav(){
		favorites.removeAllViews();
		favs = rdb.getAllFavorites();
		for (int i=0; i<favs.length; i++){
			Button b = new Button(Radio.this);
			b.setId(i);
			b.setOnClickListener(new Button.OnClickListener(){
				@Override
				public void onClick(View v) {
					int favidx = v.getId();

					tune(favs[favidx].frequency, favs[favidx].fm);
				}
			});
			b.setOnLongClickListener(new Button.OnLongClickListener(){
				@Override
				public boolean onLongClick(final View v) {
					//final String channel = ((Button)v).getText().toString();
					AlertDialog.Builder builder = new AlertDialog.Builder(Radio.this);
					builder.setTitle("Move Favorite:");
					builder.setPositiveButton("Right -->", new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				        	   rdb.moveFav(favs[v.getId()], true);
				        	   refreshfav();
				        	   dialog.dismiss();
				           }
				       });
					builder.setNeutralButton("Cancel",  new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				        	   dialog.dismiss();
				           }
				       });
					builder.setNegativeButton("<-- Left", new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				        	   rdb.moveFav(favs[v.getId()], false);
				        	   refreshfav();
				        	   dialog.dismiss();
				           }
				       });

					AlertDialog dialog = builder.create();
					dialog.show();
					return false;
				}
			});
			String favname;
			if (favs[i].fm)
				favname = Float.toString(((float) favs[i].frequency) / 100) + " FM";
			else
				favname=favs[i].frequency+" AM";
			b.setText(favname);
			favorites.addView(b);
		}
	}
}
