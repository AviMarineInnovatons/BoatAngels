package in.avimarine.boatangels.fragments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import de.hdodenhof.circleimageview.CircleImageView;
import in.avimarine.boatangels.CheckBoxTriState;
import in.avimarine.boatangels.CheckBoxTriState.State;
import in.avimarine.boatangels.R;
import in.avimarine.boatangels.activities.AddBoatActivity;
import in.avimarine.boatangels.activities.AddUserActivity;
import in.avimarine.boatangels.activities.AskInspectionActivity;
import in.avimarine.boatangels.activities.InspectBoatActivity;
import in.avimarine.boatangels.activities.InspectBoatActivity.Item;
import in.avimarine.boatangels.activities.InspectBoatActivity.ItemsListAdapter;
import in.avimarine.boatangels.activities.MainActivity;
import in.avimarine.boatangels.customViews.WeatherTableView;
import in.avimarine.boatangels.customViews.WeatherTableView.SpeedUnits;
import in.avimarine.boatangels.db.FireBase;
import in.avimarine.boatangels.db.iDb;
import in.avimarine.boatangels.db.objects.Boat;
import in.avimarine.boatangels.db.objects.Inspection;
import in.avimarine.boatangels.db.objects.Marina;
import in.avimarine.boatangels.db.objects.User;
import in.avimarine.boatangels.general.GeneralUtils;
import in.avimarine.boatangels.general.Setting;
import in.avimarine.boatangels.geographical.GeoUtils;
import in.avimarine.boatangels.geographical.OpenWeatherMap;
import in.avimarine.boatangels.geographical.Weather;
import in.avimarine.boatangels.geographical.WeatherHttpClient;
import in.avimarine.boatangels.geographical.Wind;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.w3c.dom.Text;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MyBoatFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MyBoatFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MyBoatFragment extends Fragment {
  private final iDb db = new FireBase();
  private String ownBoatUuid;
  private User currentUser = null;
  private Boat currentBoat = null;
  private Marina currentMarina = null;
  private static final String TAG = "MyBoatFragment";

  // TODO: Rename parameter arguments, choose names that match
  // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
  private static final String ARG_PARAM1 = "param1";
  private static final String ARG_PARAM2 = "param2";

  // TODO: Rename and change types of parameters
  private String mParam1;
  private String mParam2;

  private OnFragmentInteractionListener mListener;

  public MyBoatFragment() {
    // Required empty public constructor
  }

  /**
   * Use this factory method to create a new instance of
   * this fragment using the provided parameters.
   *
   * @param param1 Parameter 1.
   * @param param2 Parameter 2.
   * @return A new instance of fragment MyBoatFragment.
   */
  // TODO: Rename and change types and number of parameters
  public static MyBoatFragment newInstance(String param1, String param2) {
    MyBoatFragment fragment = new MyBoatFragment();
    Bundle args = new Bundle();
    args.putString(ARG_PARAM1, param1);
    args.putString(ARG_PARAM2, param2);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    Button ask = getActivity().findViewById(R.id.ask_inspection);
    ask.setOnClickListener(view -> {
      Intent intent = new Intent(getActivity(), AskInspectionActivity.class);
      startActivity(intent);
    });
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getArguments() != null) {
      mParam1 = getArguments().getString(ARG_PARAM1);
      mParam2 = getArguments().getString(ARG_PARAM2);
    }

  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    FirebaseAuth auth = FirebaseAuth.getInstance();
    if (auth.getCurrentUser() != null) {
      Log.d(TAG, "Logged in");
      isUserRegistered(FirebaseAuth.getInstance().getUid());

    } else {
      Log.d(TAG, "Not logged in");
    }
    return inflater.inflate(R.layout.fragment_my_boat, container, false);
  }

  // TODO: Rename method, update argument and hook method into UI event
  public void onButtonPressed(Uri uri) {
    if (mListener != null) {
      mListener.onFragmentInteraction(uri);
    }
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    if (context instanceof OnFragmentInteractionListener) {
      mListener = (OnFragmentInteractionListener) context;
    } else {
      throw new RuntimeException(context.toString()
          + " must implement OnFragmentInteractionListener");
    }
  }

  @Override
  public void onDetach() {
    super.onDetach();
    mListener = null;
  }
  private void isUserRegistered(String uid) {
    db.getUser(uid, task -> {
      if (task.isSuccessful()) {
        DocumentSnapshot document = task.getResult();
        if (!document.exists()) {
          Intent intent = new Intent(getActivity(), AddUserActivity.class);
          startActivity(intent);
        } else {
          currentUser = document.toObject(User.class);
          db.setCurrentUser(currentUser);
          Setting.setUser(getActivity(),currentUser);
          if (!currentUser.getBoats().isEmpty()) {
            ownBoatUuid = currentUser.getBoats().get(0);
            getOwnBoat(ownBoatUuid);
          } else {

          }
        }
      }
    });
  }
  private void getOwnBoat(String uuid) {
    if (currentBoat == null || !currentBoat.getUuid().equals(uuid)) {
      db.getBoat(uuid, task -> {
        if (task.isSuccessful()) {
          DocumentSnapshot document = task.getResult();
          if (document.exists()) {
            currentBoat = document.toObject(Boat.class);
              db.getLatestInspection(currentBoat.getUuid(),
                  task12 -> {
                    if (task.isSuccessful()) {
                      for (DocumentSnapshot d : task12.getResult()) {
                        updateInspectionView(d.toObject(Inspection.class));
                        break;
                      }
                    } else {
                      Log.e(TAG, "Error getting documents: ", task.getException());
                      updateInspectionView(null);
                    }
                  });
            if (!GeneralUtils.isNull(currentBoat, currentBoat.getMarinaUuid())) {
              db.getMarina(currentBoat.getMarinaUuid(),
                  task1 -> {
                    DocumentSnapshot document1 = task1.getResult();
                    if (document1.exists()) {
                      currentMarina = document1.toObject(Marina.class);
                      updateWeather(currentMarina);
                    } else {
                      Log.e(TAG, "Unable to get own marina");
                    }
                  });
            } else {
              Log.e(TAG, "Unable to get own boat");
            }
          } else {
            Log.e(TAG, "Unable to get own boat");
          }
        }
      });
    }
  }

  private void updateInspectionView(Inspection inspection) {
    if (inspection==null) {
      Log.d(TAG, "Inspection is null");
      return;
    }
    if (getActivity()!=null)
    {
      TextView title = getActivity().findViewById(R.id.inspect_boat_title);
      TextView subTitle = getActivity().findViewById(R.id.inspection_subtitle);
      ListView listView = getActivity().findViewById(R.id.listview);
      List<Item> items = initItems(inspection);
      TextView message = getActivity().findViewById(R.id.message_TextView);
      CircleImageView civ = getActivity().findViewById(R.id.boat_image);
      title.setText(inspection.boatName);
      subTitle.setText("Was inspected on " + GeneralUtils.toFormatedDateString(getActivity(),new Date(inspection.inspectionTime)) + "\nby "
          + inspection.inspectorName);
      message.setText(inspection.message);
      setBoatPhoto(civ, currentBoat.getPhotoName());
      ItemsListAdapter myItemsListAdapter;
      myItemsListAdapter = new ItemsListAdapter(getActivity(), items);
      listView.setAdapter(myItemsListAdapter);
    }

  }

  private void setBoatPhoto(CircleImageView civ, String photoName) {
    new FireBase().loadImgToImageView(getActivity(),civ,"boats/"+photoName,R.drawable.ic_no_picture_boat_icon,R.drawable.ic_no_picture_boat_icon);
  }
  private List<Item> initItems(Inspection i) {
    List<Item> ret = new ArrayList<>();
    for (Map.Entry<String,String> me: i.finding.entrySet()) {
      Item item = new Item(me.getKey(), CheckBoxTriState.State.valueOf(me.getValue()));
      ret.add(item);
    }
    return ret;
  }

  private void updateWeather(Marina m) {
    if (GeneralUtils.isNull(m, m.getLocation())) {
      Log.e(TAG, "Current Marina is null");
    }
    final OpenWeatherMap owp = new OpenWeatherMap();
    if (checkWeather(m.getWeather())) {
      updateWeatherWidget(m.getWeather());
    } else {
      new WeatherHttpClient(getActivity(), output -> {
        Weather w = owp.parseData(output);
        if (w != null) {
          updateWeatherWidget(w);
          currentMarina.setWeather(w);
          db.addMarina(currentMarina);
        }
      }).execute(
          GeoUtils.createLocation(m.getLocation().getLatitude(), m.getLocation().getLongitude()));
    }
  }
  private boolean checkWeather(Weather weather) {
    if (weather == null) {
      return false;
    }
    if (getMaxWindDaysArray(weather.getWindForecast()).size() == 6) {
      if (GeneralUtils.getMinutesDifference(weather.getLastUpdate(), GeneralUtils.now()) < 120) {
        return true;
      }
    }
    return false;
  }

  private void updateWeatherWidget(Weather w) {
    Map<Integer, Wind> daysArr = getMaxWindDaysArray(w.getWindForecast());
    ArrayList<Wind> winds = new ArrayList<>();
    for (Map.Entry<Integer, Wind> e : daysArr.entrySet()) {
      winds.add(e.getValue());
    }
    ((WeatherTableView) getActivity().findViewById(R.id.tableLayout)).setWind(winds);
    ((WeatherTableView) getActivity().findViewById(R.id.tableLayout)).setDateTime(w.getLastUpdate());
    ((WeatherTableView) getActivity().findViewById(R.id.tableLayout)).setSpeedUnits(SpeedUnits.KNOTS);
  }

  private Map<Integer, Wind> getMaxWindDaysArray(Map<Date, Wind> windForecast) {
    Map<Integer, Wind> ret = new TreeMap<>();
    int day = -1;
    double speed = 0;
    double dir;
    for (Map.Entry<Date, Wind> w : windForecast.entrySet()) {
      Calendar cal = Calendar.getInstance();
      cal.setTime(w.getKey());
      if (day == -1) {
        day = cal.get(Calendar.DAY_OF_MONTH);
        speed = w.getValue().getSpeed();
        dir = w.getValue().getDirection();
        ret.put(day, new Wind(speed, dir));
      } else {
        if (day == cal.get(Calendar.DAY_OF_MONTH)) {
          if (w.getValue().getSpeed() > speed) {
            speed = w.getValue().getSpeed();
            dir = w.getValue().getDirection();
            ret.put(day, new Wind(speed, dir));
          }
        } else {
          speed = w.getValue().getSpeed();
          dir = w.getValue().getDirection();
          day = cal.get(Calendar.DAY_OF_MONTH);
          ret.put(day, new Wind(speed, dir));
        }
      }
    }
    return ret;
  }
  /**
   * This interface must be implemented by activities that contain this
   * fragment to allow an interaction in this fragment to be communicated
   * to the activity and potentially other fragments contained in that
   * activity.
   * <p>
   * See the Android Training lesson <a href=
   * "http://developer.android.com/training/basics/fragments/communicating.html"
   * >Communicating with Other Fragments</a> for more information.
   */
  public interface OnFragmentInteractionListener {

    // TODO: Update argument type and name
    void onFragmentInteraction(Uri uri);
  }
}
