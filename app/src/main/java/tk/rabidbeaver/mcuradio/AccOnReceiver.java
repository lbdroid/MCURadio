package tk.rabidbeaver.mcuradio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AccOnReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent){
        RadioDB rdb = new RadioDB(context);
        int lastFM = rdb.getLastFM();
        int lastAM = rdb.getLastAM();
        boolean isLastFM = rdb.isLastBandFM();

        Intent i = new Intent();
        i.setAction("tk.rabidbeaver.radiocontroller.BAND");
        i.putExtra("BAND", isLastFM?"FM":"AM");
        context.sendBroadcast(i);

        i = new Intent();
        i.setAction("tk.rabidbeaver.radiocontroller.TUNE");
        i.putExtra("FREQ", isLastFM?lastFM:lastAM);
        context.sendBroadcast(i);
    }
}
