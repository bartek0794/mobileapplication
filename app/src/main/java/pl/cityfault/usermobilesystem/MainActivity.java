package pl.cityfault.usermobilesystem;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private final static String createFaultURL = "https://defectsmanagement.herokuapp.com/api/createDefect";
    private static final int REQUEST_PERMISSIONS = 1;

    private EditText email, description, title;
    private Button reportFaultButton, capturePhotoButton;
    private ImageButton langPlButton, langEnButton;
    private RestTemplate restTemplate;
    private Spinner spinnerCity;

    private List<String> departments;
    private Fault fault;

    private File file;

    private double longitude = 0;
    private double latitude = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        initializeComponents();
        restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        checkPermission();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            departments = extras.getStringArrayList("departments");
        }

        getDepartments();
        fault = new Fault();

        if(checkIfAccessFineLocationisAvailable()) {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Location location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location != null) {
                longitude = location.getLongitude();
                latitude = location.getLatitude();
            }
        }
    }

    private void getDepartments() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, departments);
        spinnerCity.setAdapter(adapter);
    }

    private void initializeComponents() {
        spinnerCity = (Spinner) findViewById(R.id.chooseDepartment);
        email = (EditText) findViewById(R.id.emailAddressInput);
        title = (EditText) findViewById(R.id.titleInput);
        description = (EditText) findViewById(R.id.faultDescriptionInput);
        reportFaultButton = (Button) findViewById(R.id.reportNewFault);
        reportFaultButton.setOnClickListener(this);
        capturePhotoButton = (Button) findViewById(R.id.capturePhoto);
        capturePhotoButton.setOnClickListener(this);
        langPlButton = (ImageButton) findViewById(R.id.langPL);
        langPlButton.setOnClickListener(this);
        langEnButton = (ImageButton) findViewById(R.id.langEN);
        langEnButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.reportNewFault:
                if (isValid()) {
                    if (CheckConnection.isConnectedToServer()) {
                        fault.setDescription(description.getText().toString());
                        fault.setTitle(title.getText().toString());
                        fault.setEmail(email.getText().toString());
                        fault.setDepartment(SplashActivity.searchDepartment(spinnerCity.getSelectedItem().toString()));
                        fault.setLatitude(latitude);
                        fault.setLongitude(longitude);
                        restTemplate.postForObject(createFaultURL, fault, Fault.class);
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.toastDefectReported), Toast.LENGTH_SHORT).show();
                        clearFields();
                        fault = new Fault();
                    } else {
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.connectionError), Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case R.id.capturePhoto:
                dispatchTakePictureIntent();
                break;
            case R.id.langPL:
                updateResources(this, "pl");
                break;
            case R.id.langEN:
                updateResources(this, "en");
                break;
        }
    }

    private void dispatchTakePictureIntent() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        file = new File(Environment.getExternalStorageDirectory() + File.separator + "image.jpg");
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
        startActivityForResult(intent, REQUEST_PERMISSIONS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PERMISSIONS && resultCode == RESULT_OK) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
            bmp.compress(Bitmap.CompressFormat.JPEG, 20, stream);
            byte[] byteArray = stream.toByteArray();
            Photo photo = new Photo();
            photo.setPhoto(byteArray);
            fault.setPhoto(photo);
        }
    }

    public void clearFields() {
        email.setText("");
        description.setText("");
        title.setText("");
    }

    private boolean isValid() {
        if (!isValidEmail(email.getText().toString())) {
            email.setError(getResources().getString(R.string.toastInvalidEmail));
            return false;
        }
        if (!isValidDescription(description.getText().toString())) {
            description.setError(getResources().getString(R.string.toastInvalidDescription));
            return false;
        }
        if (!isValidTitle(title.getText().toString())) {
            title.setError(getResources().getString(R.string.toastInvalidTitle));
            return false;
        }
        if (fault.getPhoto() == null) {
            capturePhotoButton.setError(getResources().getString(R.string.toastInvalidPhoto));
            return false;
        }
        return true;
    }

    private boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isValidDescription(String description) {
        return description != null && description.length() >= 5 && description.length() <= 100 ? true : false;
    }

    private boolean isValidTitle(String title) {
        return title != null && title.length() >= 5 && title.length() <= 50 ? true : false;
    }

    public void checkPermission() {
        List<String> listPermissionsNeeded = new ArrayList<>();

        int cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int locationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        int storagePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (cameraPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.CAMERA);
        }
        if (locationPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (storagePermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), REQUEST_PERMISSIONS);
        }
    }
    public boolean checkIfAccessFineLocationisAvailable() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        boolean isGranted = true;
        if (grantResults.length > 0) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    isGranted = false;
                }
            }
        }

        if (!isGranted) {
            capturePhotoButton.setEnabled(false);
            reportFaultButton.setEnabled(false);
        }
        return;
    }

    private void updateResources(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
        this.recreate();
    }
}