package edu.stlawu.montyhall;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.Image;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.reflect.Array;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.Context.MODE_PRIVATE;


/**
 * A simple {@link Fragment} subclass.
 */
public class GameFragment extends Fragment {

    // Instance variables
    private MontyDoors door1 = null;
    private MontyDoors door2 = null;
    private MontyDoors door3 = null;

    private TextView tv_wins = null;
    private TextView tv_wins_p = null;
    private TextView tv_losses = null;
    private TextView tv_losses_p = null;
    private TextView tv_total = null;
    private TextView tv_total_p = null;
    private TextView tv_prompt = null;

    private Button bt_switch = null;
    private Button bt_stay = null;

    private int picked;
    private String[] doorValues = null;
    private int doorWithCar;
    View gameView = null;

    // Variables to save state
    private int goatDoorIndex = 0;
    private SharedPreferences.Editor prefs = null;

    // Instance variable counts to be saved in preferences
    private int wins;
    private int losses;

    Animation slideUp;

    public AudioAttributes aa = null;
    private SoundPool soundPool = null;
    private int goatSound = 0;

    public GameFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        gameView = inflater.inflate(R.layout.fragment_game, container, false);

        prefs = this.getActivity().getPreferences(MODE_PRIVATE).edit();

        // Create the doors
        doorValues = orderDoors();
        door1 = new MontyDoors((ImageButton) gameView.findViewById(R.id.door1), doorValues[0], 0);
        door2 = new MontyDoors((ImageButton) gameView.findViewById(R.id.door2), doorValues[1], 1);
        door3 = new MontyDoors((ImageButton) gameView.findViewById(R.id.door3), doorValues[2], 2);

        // Find all the UI elements by ID
        tv_wins = gameView.findViewById(R.id.wins);
        tv_wins_p = gameView.findViewById(R.id.wins_p);
        tv_losses = gameView.findViewById(R.id.losses);
        tv_losses_p = gameView.findViewById(R.id.losses_p);
        tv_total = gameView.findViewById(R.id.total);
        tv_total_p = gameView.findViewById(R.id.total_p);
        tv_prompt = gameView.findViewById(R.id.prompt);

        bt_stay = gameView.findViewById(R.id.bt_stay);
        bt_switch = gameView.findViewById(R.id.bt_switch);



        prefs.putBoolean("UNFINISHED", false);

        // Event listener to left door
        door1.getDoor().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                door1_func();
            }
        });

        // Event listener middle door
        door2.getDoor().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                door2_func();
            }
        });

        // Event listener right door
        door3.getDoor().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                door3_func();
            }
        });

        bt_stay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                prefs.putBoolean("UNFINISHED", false);
                if (doorWithCar == picked){
                    wins++;
                    delayMessageChange(tv_prompt.getId(), R.string.winner, 3000);                } else {
                    losses++;
                    delayMessageChange(tv_prompt.getId(), R.string.loser, 3000);                }
                updateGrid();
                bt_stay.setEnabled(false);
                bt_switch.setEnabled(false);
                countDownDoor(door1);
                countDownDoor(door2);
                countDownDoor(door3);
            }
        });

        bt_switch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                prefs.putBoolean("UNFINISHED", false);
                if (doorWithCar == picked){
                    losses++;
                    delayMessageChange(tv_prompt.getId(), R.string.loser, 3000);
                } else {
                    wins++;
                    delayMessageChange(tv_prompt.getId(), R.string.winner, 3000);
                }
                updateGrid();
                bt_stay.setEnabled(false);
                bt_switch.setEnabled(false);
                countDownDoor(door1);
                countDownDoor(door2);
                countDownDoor(door3);
            }
        });

        return gameView;
    }

    public void startSlideUpAnimation(View view) {
        bt_switch.startAnimation(slideUp);
        bt_stay.startAnimation(slideUp);
    }

    @Override
    public void onStart() {
        super.onStart();

        // Restore wins/losses if they exist, otherwise 0
        this.wins = this.getActivity().getPreferences(MODE_PRIVATE).getInt("WINS", 0);
        this.losses = this.getActivity().getPreferences(MODE_PRIVATE).getInt("LOSSES", 0);

        this.aa = new AudioAttributes
                .Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_GAME)
                .build();

        this.soundPool = new SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(aa)
                .build();
        this.goatSound = this.soundPool.load(
                getContext(), R.raw.goat, 1);

        slideUp = AnimationUtils.loadAnimation(getContext(),
                R.anim.slide_up_animation);

        //////////// Restore values if the Continue button has been clicked
        SharedPreferences restores = getContext().getSharedPreferences(MainFragment.PREF_NAME, MODE_PRIVATE);
        if (!restores.getBoolean(MainFragment.NEW_CLICKED, true) && restores.getBoolean("UNFINISHED", true)) {
            Log.d("PREFERENCES", "In restore");
            doorValues = orderDoors();

            doorValues[0] = restores.getString("DOOR_VALUES_0", "CAR");
            doorValues[1] = restores.getString("DOOR_VALUES_1", "GOAT");
            doorValues[2] = restores.getString("DOOR_VALUES_2", "GOAT");
            doorWithCar = restores.getInt("DOOR_WITH_CAR", 2);
            goatDoorIndex = restores.getInt("GOAT_DOOR_INDEX", 2);
            picked = restores.getInt("PICKED", 2);

            Log.d("Preference stuff", doorValues[0]);
            Log.d("Preference stuff", doorValues[1]);
            Log.d("Preference stuff", doorValues[2]);
            Log.d("Preference stuff", Integer.toString(doorWithCar));
            Log.d("Preference stuff", Integer.toString(goatDoorIndex));
            Log.d("Preference stuff", Integer.toString(picked));


            door1 = new MontyDoors((ImageButton) gameView.findViewById(R.id.door1), doorValues[0], 0);
            door2 = new MontyDoors((ImageButton) gameView.findViewById(R.id.door2), doorValues[1], 1);
            door3 = new MontyDoors((ImageButton) gameView.findViewById(R.id.door3), doorValues[2], 2);

            door1.getDoor().setEnabled(false);
            door2.getDoor().setEnabled(false);
            door3.getDoor().setEnabled(false);

            if (picked == 0){
                door1.getDoor().setImageResource(R.drawable.closed_door_chosen);
            } else if (picked == 1) {
                door2.getDoor().setImageResource(R.drawable.closed_door_chosen);
            } else if (picked == 2) {
                door3.getDoor().setImageResource(R.drawable.closed_door_chosen);
            }

            if (goatDoorIndex == 0){
                doorChosen(door1);
            } else if (goatDoorIndex == 1) {
                doorChosen(door2);
            } else if (goatDoorIndex == 2) {
                doorChosen(door3);
            }
        }

        updateGrid();

    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("PREFERENCES", "In onpause");

        SharedPreferences.Editor saves = this.getActivity().getPreferences(MODE_PRIVATE).edit();

        // Save the state of variables
        saves.putInt("PICKED", this.picked).apply();
        saves.putString("DOOR_VALUES_0", this.doorValues[0]).apply();
        saves.putString("DOOR_VALUES_1", this.doorValues[1]).apply();
        saves.putString("DOOR_VALUES_2", this.doorValues[2]).apply();
        saves.putInt("DOOR_WITH_CAR", this.doorWithCar).apply();

        saves.putInt("GOAT_DOOR_INDEX", this.goatDoorIndex).apply();


        // Save wins/losses
        saves.putInt("WINS", this.wins).commit();
        saves.putInt("LOSSES", this.losses).commit();
    }

    private void door1_func() {
        doorChosen(door1);
    }

    private void door2_func() {
        doorChosen(door2);
    }

    private void door3_func() {
        doorChosen(door3);
    }

    private void updateGrid() {
        int total = wins + losses;
        if (wins != 0){
            float win_p = ((float) wins / total) * 100;
            tv_wins.setText(Integer.toString(wins));
            tv_wins_p.setText(win_p + "%");
        } else {
            tv_wins.setText("0");
            tv_wins_p.setText("0%");
        }
        if (losses != 0){
            float loss_p = ((float) losses / total) * 100;
            tv_losses.setText(Integer.toString(losses));
            tv_losses_p.setText(loss_p + "%");
        } else {
            tv_losses.setText("0");
            tv_losses_p.setText("0%");
        }
        tv_total.setText(Integer.toString(total));
        tv_total_p.setText("100%");
    }

    private String[] orderDoors() {
        Random r = new Random();
        String[] doors = {"GOAT", "GOAT", "GOAT"};
        doors[r.nextInt(3)] = "CAR";
        return doors;
    }

    private void countDownDoor(final MontyDoors count) {
        count.getDoor().setImageResource(R.drawable.three);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                count.getDoor().setImageResource(R.drawable.two);
            }
        }, 1000);

        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                count.getDoor().setImageResource(R.drawable.one);
            }
        }, 2000);

        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                count.setOpen();
            }
        }, 3000);
    }

    private void doorChosen(MontyDoors choice) {
        this.getActivity().getPreferences(MODE_PRIVATE).edit().putBoolean("UNFINISHED", true);
        choice.getDoor().setImageResource(R.drawable.closed_door_chosen);
        bt_switch.setVisibility(View.VISIBLE);
        bt_stay.setVisibility(View.VISIBLE);
        startSlideUpAnimation(bt_stay);
        tv_prompt.setText(R.string.switch_stay);
        soundPool.play(goatSound, 1f,
                1f, 1, 0, 1f);
        door1.getDoor().setEnabled(false);
        door2.getDoor().setEnabled(false);
        door3.getDoor().setEnabled(false);
        this.picked = choice.getIndex();

        for (int i = 0; i < 3; i++) {
            if (doorValues[i].equals("CAR")){
                doorWithCar = i;
            }
        }
        Random r = new Random();
        MontyDoors goatDoor = null;
        int possibleGoatDoor1 = (doorWithCar + 1) % 3;
        int possibleGoatDoor2 = (doorWithCar + 2) % 3;
        int goatDoorIndex = 0;  // Initialized to 0 bc java complains that it might
                                // now be initialized, but logically it must be
        if (r.nextInt(2) == 1) {
            if (possibleGoatDoor1 != choice.getIndex()){
                goatDoorIndex = possibleGoatDoor1;
            }
            if (possibleGoatDoor2 != choice.getIndex()){
                goatDoorIndex = possibleGoatDoor2;
            }
        } else {
            if (possibleGoatDoor2 != choice.getIndex()){
                goatDoorIndex = possibleGoatDoor2;
            }
            if (possibleGoatDoor1 != choice.getIndex()){
                goatDoorIndex = possibleGoatDoor1;
            }
        }
        if (goatDoorIndex == 0) {
            goatDoor = door1;
        } else if (goatDoorIndex == 1) {
            goatDoor = door2;
        } else {
            goatDoor = door3;
        }

        countDownDoor(goatDoor);
        choice.getDoor().setImageResource(R.drawable.closed_door_chosen);

        goatDoorIndex = goatDoor.getIndex();
    }

    public void delayMessageChange(final int tv_id, final int newText, int delay) {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                TextView text = getActivity().findViewById(tv_id);
                text.setText(newText);
            }
        }, delay);
    }
}
