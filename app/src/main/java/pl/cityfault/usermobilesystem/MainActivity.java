package pl.cityfault.usermobilesystem;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private EditText email, description;
    private Button reportFaultButton, capturePhotoButton;
    private RestTemplate restTemplate;
    private Spinner spinnerCity;
    private final static String createFaultURL = "https://defectsmanagement.herokuapp.com/createFault";
    private final static String getAllDepartmentsURL = "https://defectsmanagement.herokuapp.com/getAllDepartments";
    private Department[] departments;
    private Fault fault;

    private LocationManager locationManager;
    Location location;

    static final int REQUEST_IMAGE_CAPTURE = 1;
    File file;

    double longitude;
    double latitude;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        spinnerCity = (Spinner) findViewById(R.id.chooseDepartment);
        email = (EditText) findViewById(R.id.emailAddressInput);
        description = (EditText) findViewById(R.id.faultDescriptionInput);
        restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if(isLocationEnabled()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
            }
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);

            ResponseEntity<Department[]> response = restTemplate.getForEntity(getAllDepartmentsURL, Department[].class);
            departments = response.getBody();
        }
        else {
            showAlert();
        }

       fault = new Fault();

       List<String> departmentsName = new ArrayList<String>();
        for (int i = 0; i < departments.length; i++) {
            departmentsName.add(departments[i].getDepartmentName());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, departmentsName);
        spinnerCity.setAdapter(adapter);

        reportFaultButton = (Button) findViewById(R.id.reportNewFault);
        reportFaultButton.setOnClickListener(this);
        capturePhotoButton = (Button) findViewById(R.id.capturePhoto);
        capturePhotoButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.reportNewFault:
                if(isValid()) {
                    fault.setDescription(description.getText().toString());
                    fault.setEmail(email.getText().toString());
                    fault.setDepartment(searchDepartment(spinnerCity.getSelectedItem().toString()));
                    fault.setLatitude(latitude);
                    fault.setLongitude(longitude);

                    restTemplate.postForObject(createFaultURL, fault, Fault.class);
                    clearFields();
                    fault = new Fault();
                }
                break;
            case R.id.capturePhoto:
                dispatchTakePictureIntent();

       }
    }

    private void dispatchTakePictureIntent() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        file = new File(Environment.getExternalStorageDirectory()+File.separator + "image.jpg");
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
            bmp.compress(Bitmap.CompressFormat.JPEG, 20, stream);
            byte[] byteArray = stream.toByteArray();
            fault.setPhoto(byteArray);
        }
    }

    public Department searchDepartment(String name) {
        for(int i = 0; i < departments.length; i++) {
            if(departments[i].getDepartmentName().equals(name)) {
                return departments[i];
            }
        }
        return new Department();
    }

    public void clearFields() {
        email.setText("");
        description.setText("");
    }

    private boolean isLocationEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private void showAlert() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Enable Location")
                .setMessage("Your Locations Settings is set to 'Off'.\nPlease Enable Location to " +
                        "use this app")
                .setPositiveButton("Location Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(myIntent);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    }
                });
        dialog.show();
    }

    private final LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            longitude = location.getLongitude();
            latitude = location.getLatitude();
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) { }

        @Override
        public void onProviderEnabled(String s) { }

        @Override
        public void onProviderDisabled(String s) { }
    };

    private boolean isValid() {
        if (!isValidEmail(email.getText().toString())) {
            email.setError("Field must be not empty [5-50]");
            return false;
        }
        if (!isValidDescription(description.getText().toString())) {
            description.setError("Field must be not empty [5-100]");
            return false;
        }
        if (fault.getPhoto() == null) {
            capturePhotoButton.setError("You must capture photo");
            return false;
        }
        return true;
    }

    private boolean isValidEmail(String toValid) {
        return toValid != null && toValid.length() >= 5 && toValid.length() <= 50 ?  true : false;
    }


    private boolean isValidDescription(String toValid) {
        return toValid != null && toValid.length() >= 5 && toValid.length() <= 100 ?  true : false;
    }
}
