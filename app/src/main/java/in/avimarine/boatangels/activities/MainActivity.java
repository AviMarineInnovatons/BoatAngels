package in.avimarine.boatangels.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.github.pwittchen.weathericonview.WeatherIconView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.GeoPoint;
import in.avimarine.boatangels.R;
import in.avimarine.boatangels.db.FireBase;
import in.avimarine.boatangels.db.iDb;
import in.avimarine.boatangels.db.objects.Marina;
import in.avimarine.boatangels.db.objects.User;
import in.avimarine.boatangels.general.GeneralUtils;
import in.avimarine.boatangels.geographical.GeoUtils;
import in.avimarine.boatangels.geographical.OpenWeatherMap;
import in.avimarine.boatangels.geographical.Weather;
import in.avimarine.boatangels.geographical.Weather.Wind;
import in.avimarine.boatangels.geographical.WeatherHttpClient;
import in.avimarine.boatangels.geographical.AsyncResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity {

  private static final String TAG = "MainActivity";
  private static final int RC_SIGN_IN = 123;
  @SuppressWarnings("WeakerAccess")
  @BindView(R.id.welcome_message_textview)
  TextView welcomeTv;
  @SuppressWarnings("WeakerAccess")
  @BindView(R.id.sign_out_btn)
  Button signoutBtn;
  @SuppressWarnings("WeakerAccess")
  @BindView(R.id.inspect_boat_btn)
  Button inspectBoatBtn;
  @SuppressWarnings("WeakerAccess")
  @BindView(R.id.add_boat_btn)
  Button addBoatBtn;
  @SuppressWarnings("WeakerAccess")
  @BindView(R.id.show_inspections_btn)
  Button showInspectionBtn;
  @SuppressWarnings("WeakerAccess")
  @BindView(R.id.ask_inspection)
  Button askInspectionBtn;

  private final iDb db = new FireBase();
  private String ownBoatUuid;
  private User currentUser = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    ButterKnife.bind(this);
    FirebaseAuth auth = FirebaseAuth.getInstance();

    if (auth.getCurrentUser() != null) {
      Log.d(TAG, "Logged in");
      isUserRegistered(FirebaseAuth.getInstance().getUid());
      welcomeTv.setText(String
          .format(getString(R.string.welcome_message), auth.getCurrentUser().getDisplayName()));
      signoutBtn.setEnabled(true);
    } else {
      Log.d(TAG, "Not logged in");
      startActivityForResult(
          AuthUI.getInstance()
              .createSignInIntentBuilder()
              .setAvailableProviders(
                  Arrays.asList(new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
                      new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()))
              .build(),
          RC_SIGN_IN);
    }
    final OpenWeatherMap owp = new OpenWeatherMap();
    new WeatherHttpClient(output -> {
      Weather w = owp.parseData(output);
      Log.d(TAG,w.windForecast.toString());
      Map<Integer,Wind> daysArr = getMaxWindDaysArray(w.windForecast);
      int i=0;
      TableLayout tblLayout = (TableLayout)findViewById(R.id.tableLayout);
      TableRow row = (TableRow)tblLayout.getChildAt(0); // Here get row id depending on number of row
      LayoutInflater inflater = (LayoutInflater)      getSystemService(LAYOUT_INFLATER_SERVICE);
      for (Map.Entry<Integer,Wind> e: daysArr.entrySet()){
        if (e!=null&&e.getValue()!=null){
          View childLayout = inflater.inflate(R.layout.weather_table_col, null);
          WeatherIconView wiv = (WeatherIconView) ((ViewGroup)childLayout).getChildAt(0);
          TextView tv = (TextView) ((ViewGroup)childLayout).getChildAt(1);
          setWeatherIcon(wiv,tv,e.getValue().getDirection(),e.getValue().getSpeed());
          row.addView(childLayout);
        }
        i++;
      }

    }).execute(GeoUtils.createLocation(32.56,34.94));

    //addMarinas();
  }

  private void setWeatherIcon(WeatherIconView weatherIcon, TextView tv, Float direction, Float speed) {
    if (!GeneralUtils.isNull(weatherIcon,direction)){
      weatherIcon.setIconResource(getString(R.string.wi_wind_direction));
      weatherIcon.setRotation(direction+180);
      if (speed>7.5){
        weatherIcon.setIconColor(Color.RED);
      }
      weatherIcon.setVisibility(View.VISIBLE);
      tv.setText(Math.round(speed*1.94)+"kn");
    }
    else weatherIcon.setVisibility(View.INVISIBLE);

  }

  private Map<Integer,Wind> getMaxWindDaysArray(Map<Date, Wind> windForecast) {
    Map<Integer,Wind> ret = new TreeMap<>();
    int day = -1;
    double speed = 0;
    double dir = 0;
    for (Map.Entry<Date,Wind> w : windForecast.entrySet()){
      Calendar cal = Calendar.getInstance();
      cal.setTime(w.getKey());
      if (day == -1){
        day = cal.get(Calendar.DAY_OF_MONTH);
        speed = w.getValue().getSpeed();
        dir = w.getValue().getDirection();
        ret.put(day,new Weather().new Wind(speed,dir));
      }else{
        if (day==cal.get(Calendar.DAY_OF_MONTH)){
          if(w.getValue().getSpeed()>speed){
            speed= w.getValue().getSpeed();
            dir = w.getValue().getDirection();
            ret.put(day,new Weather().new Wind(speed,dir));
          }
        } else{
          speed= w.getValue().getSpeed();
          dir = w.getValue().getDirection();
          day = cal.get(Calendar.DAY_OF_MONTH);
          ret.put(day,new Weather().new Wind(speed,dir));
        }
      }
    }
    return ret;
  }

  @Override
  protected void onStart() {
    super.onStart();
    if (FirebaseAuth.getInstance().getUid() != null) {
      isUserRegistered(FirebaseAuth.getInstance().getUid());
    }
  }

  @OnClick(R.id.sign_out_btn)
  public void signoutBtnClick(View v) {
    AuthUI.getInstance()
        .signOut(MainActivity.this)
        .addOnCompleteListener(new OnCompleteListener<Void>() {
          public void onComplete(@NonNull Task<Void> task) {
            signoutBtn.setEnabled(false);
            startActivityForResult(
                AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(
                        Arrays
                            .asList(new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
                                new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()))
                    .build(),
                RC_SIGN_IN);
          }
        });
  }

  @OnClick(R.id.add_boat_btn)
  public void addBoatBtnClick(View v) {
    Intent intent = new Intent(MainActivity.this, AddBoatActivity.class);
    startActivity(intent);
  }

  @OnClick(R.id.inspect_boat_btn)
  public void inspectBtnClick(View v) {
    Intent intent = new Intent(MainActivity.this, BoatForInspectionActivity.class);
    startActivity(intent);
  }

  @OnClick(R.id.show_inspections_btn)
  public void showInspectionsBtnClick(View v) {
    Intent intent = new Intent(MainActivity.this, InspectionsListActivity.class);
    intent.putExtra(getString(R.string.intent_extra_boat_uuid), ownBoatUuid);
    startActivity(intent);
  }
  @OnClick(R.id.ask_inspection)
  public void ask(View v) {
    Intent intent = new Intent(MainActivity.this, AskInspectionActivity.class);
    startActivity(intent);

  }
  public void isUserRegistered(String uid) {
    db.getUser(uid, new OnCompleteListener<DocumentSnapshot>() {
      @Override
      public void onComplete(@NonNull Task<DocumentSnapshot> task) {
        if (task.isSuccessful()) {
          DocumentSnapshot document = task.getResult();
          if (!document.exists()) {
            Intent intent = new Intent(MainActivity.this, AddUserActivity.class);
            startActivity(intent);
          }
          else{
            currentUser = document.toObject(User.class);
            db.setCurrentUser(currentUser);
            welcomeTv.setText(getString(R.string.welcome_message,currentUser.getDisplayName()));
            if (!currentUser.getBoats().isEmpty()) {
              addBoatBtn.setEnabled(false);
              showInspectionBtn.setEnabled(true);
              askInspectionBtn.setEnabled(true);
              ownBoatUuid = currentUser.getBoats().get(0);
            }
            else {
              addBoatBtn.setEnabled(true);
              showInspectionBtn.setEnabled(false);
              askInspectionBtn.setEnabled(false);
            }
          }
        }
      }
    });
  }

  /***
   * For setting first marina db. Don't call!
   */
  private void addMarinas() {
    Marina m = new Marina();
    m.name = "Shavit, Haifa";
    m.country = "Israel";
    m.location = new GeoPoint(32.805672, 35.030550);
    m.setFirstAddedTime(new Date());
    m.setLastUpdate(new Date());
    db.addMarina(m);
    m = new Marina();
    m.name = "Herzliya";
    m.country = "Israel";
    m.location = new GeoPoint(32.162881, 34.795601);
    m.setFirstAddedTime(new Date());
    m.setLastUpdate(new Date());
    db.addMarina(m);
    m = new Marina();
    m.name = "Tel-Aviv";
    m.country = "Israel";
    m.location = new GeoPoint(32.086349, 34.767430);
    m.setFirstAddedTime(new Date());
    m.setLastUpdate(new Date());
    db.addMarina(m);
    m = new Marina();
    m.name = "Ashdod";
    m.country = "Israel";
    m.location = new GeoPoint(31.795030, 34.627701);
    m.setFirstAddedTime(new Date());
    m.setLastUpdate(new Date());
    db.addMarina(m);
    m = new Marina();
    m.name = "Ashkelon";
    m.country = "Israel";
    m.location = new GeoPoint(31.682364, 34.555713);
    m.setFirstAddedTime(new Date());
    m.setLastUpdate(new Date());
    db.addMarina(m);
  }


  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    // RC_SIGN_IN is the request code you passed into startActivityForResult(...) when starting the sign in flow.
    if (requestCode == RC_SIGN_IN) {
      IdpResponse response = IdpResponse.fromResultIntent(data);

      // Successfully signed in
      if (resultCode == RESULT_OK) {
        Log.d(TAG, "Logged in!!");
        if (FirebaseAuth.getInstance() != null
            && FirebaseAuth.getInstance().getCurrentUser() != null) {
          isUserRegistered(FirebaseAuth.getInstance().getUid());


        }
        signoutBtn.setEnabled(true);
        return;
      } else {
        // Sign in failed
        if (response == null) {
          // User pressed back button
          Log.d(TAG, "Log in failure: User pressed back button");
          return;
        }

        if (response.getErrorCode() == ErrorCodes.NO_NETWORK) {
          Log.d(TAG, "Log in failure: No network");
          return;
        }

        if (response.getErrorCode() == ErrorCodes.UNKNOWN_ERROR) {
          Log.d(TAG, "Log in failure: Unknown error");
          return;
        }
      }

      Log.d(TAG, "Log in failure: Unknown login response");
    }
  }
}
