package com.example.topics;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.topics.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS = "lifecycle_demo";
    private static final String SAVED_COUNT = "saved_count";
    private static final String PERSISTENT_COUNT = "persistent_count";

    private ActivityMainBinding binding;
    private int memoryCount;
    private int savedCount;
    private int persistentCount;
    private SharedPreferences preferences;

    /**
     * Creates the current Activity View tree and connects the screen's actions.
     *
     * @param savedInstanceState prior small UI snapshot, or null for a fresh launch
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        persistentCount = preferences.getInt(PERSISTENT_COUNT, 0);
        if (savedInstanceState != null) {
            savedCount = savedInstanceState.getInt(SAVED_COUNT);
        }
        binding.increment.setOnClickListener(v -> {
            memoryCount++;
            savedCount++;
            persistentCount++;
            // Persist the model value, never the formatted TextView text.
            preferences.edit().putInt(PERSISTENT_COUNT, persistentCount).apply();
            renderCounts();
        });
        binding.reset.setOnClickListener(v -> {
            memoryCount = 0;
            savedCount = 0;
            persistentCount = 0;
            preferences.edit().remove(PERSISTENT_COUNT).apply();
            renderCounts();
        });
        renderCounts();
    }

    /**
     * Copies this component's small UI counter into Android's restoration Bundle.
     * This is a restoration snapshot, not durable storage for a fresh app launch.
     *
     * @param outState destination delivered to a later restored instance
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(SAVED_COUNT, savedCount);
        super.onSaveInstanceState(outState);
    }

    /**
     * Displays the three counters from their owning state.
     * Rendering does not increment, restore, or write any counter.
     */
    private void renderCounts() {
        binding.memoryValue.setText(getString(R.string.memory_value, memoryCount));
        binding.savedValue.setText(getString(R.string.saved_value, savedCount));
        binding.persistentValue.setText(getString(R.string.persistent_value, persistentCount));
    }
}
