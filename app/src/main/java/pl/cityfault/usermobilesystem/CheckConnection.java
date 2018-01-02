package pl.cityfault.usermobilesystem;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.widget.Toast;

import java.net.URL;
import java.net.URLConnection;

class CheckConnection extends AsyncTask<Void, Integer, Boolean> {
    private int isFirstTime = 0;
    private OnTaskCompleted taskCompleted;
    private Context context;

    public CheckConnection(OnTaskCompleted activityContext, Context ctx){
        this.taskCompleted = activityContext;
        this.context = ctx;
    }
    @Override
    protected Boolean doInBackground(Void... params) {
        while(!isConnectedToServer()) {
            if(isFirstTime == 0) {
                Handler handler =  new Handler(context.getMainLooper());
                handler.post( new Runnable(){
                    public void run(){
                        Toast.makeText(context, context.getResources().getString(R.string.connectionError), Toast.LENGTH_LONG).show();
                    }
                });
                isFirstTime = -1;
            }
        }
        return true;
    }

    protected void onPostExecute(Boolean result) {
        taskCompleted.onTaskCompleted(result);
    }

    public static boolean isConnectedToServer() {
        try {
            URL myUrl = new URL("https://z.com");
            URLConnection connection = myUrl.openConnection();
            connection.setConnectTimeout(300);
            connection.connect();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}