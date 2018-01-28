package pl.cityfault.usermobilesystem;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import java.util.ArrayList;

public class SplashActivity extends Activity implements OnTaskCompleted {

    private final int SPLASH_DISPLAY_LENGHT = 3000;
    private final static String getAllDepartmentsURL = "https://defectsmanagement.herokuapp.com/api/getAllDepartments";
    private static Department[] departments;

    RestTemplate restTemplate;
    ArrayList<String> departmentsName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        departmentsName = new ArrayList<String>();
        new CheckConnection(SplashActivity.this, SplashActivity.this).execute();
    }

    @Override
    public void onTaskCompleted(Boolean result){
        if(result) {
            backgroundJob();
        }
    }

    public void backgroundJob() {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    ResponseEntity<Department[]> response = restTemplate.getForEntity(getAllDepartmentsURL, Department[].class);
                    departments = response.getBody();

                    for (int i = 0; i < departments.length; i++) {
                        departmentsName.add(departments[i].getName());
                    }

                    if (departmentsName != null) {
                        Intent mainIntent = new Intent(SplashActivity.this, MainActivity.class);
                        mainIntent.putStringArrayListExtra("departments", departmentsName);
                        SplashActivity.this.startActivity(mainIntent);
                        SplashActivity.this.finish();
                    }
                }
            }, SPLASH_DISPLAY_LENGHT);
    }

    public static Department searchDepartment(String name) {
        for(int i = 0; i < departments.length; i++) {
            if(departments[i].getName().equals(name)) {
                return departments[i];
            }
        }
        return new Department();
    }
}
