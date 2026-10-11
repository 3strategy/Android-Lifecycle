package com.example.topics;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.topics.databinding.FragmentLifecycleBinding;

/** Demonstrates that a Fragment and its View have separate lifetimes. */
public final class LifecycleFragment extends Fragment {
    private static final String TAG = "LifecycleDemo";
    private static final String SAVED_COUNT = "fragment_count";

    private FragmentLifecycleBinding binding;
    private int count;

    /**
     * Creates the current View tree; the Fragment object may already exist.
     *
     * @param inflater creates Views using the host theme
     * @param container parent used for layout parameters; do not attach yet
     * @param savedInstanceState prior UI state, or null on a fresh creation
     * @return the root of this new View tree
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "Fragment " + System.identityHashCode(this) + " onCreateView");
        // The manager attaches the root; attaching here would attach it twice.
        binding = FragmentLifecycleBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Restores the counter and connects actions after the current binding exists.
     * A detach/back cycle retains the field when no saved Bundle is supplied.
     *
     * @param view the newly created root
     * @param savedInstanceState saved counter when Android restores this Fragment
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "Fragment " + System.identityHashCode(this) + " onViewCreated");
        if (savedInstanceState != null) {
            count = savedInstanceState.getInt(SAVED_COUNT);
        }
        binding.fragmentIncrement.setOnClickListener(v -> {
            count++;
            renderCount();
        });
        binding.detachView.setOnClickListener(v -> getParentFragmentManager()
                .beginTransaction()
                .detach(this)
                .addToBackStack("detached_view")
                .commit());
        renderCount();
    }

    /**
     * Copies this component's small UI counter into Android's restoration Bundle.
     * This is a restoration snapshot, not durable storage for a fresh app launch.
     *
     * @param outState destination delivered to a later restored instance
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(SAVED_COUNT, count);
        super.onSaveInstanceState(outState);
    }

    /**
     * Releases references to the old View tree while the Fragment may stay alive.
     */
    @Override
    public void onDestroyView() {
        Log.d(TAG, "Fragment " + System.identityHashCode(this)
                + " onDestroyView; clearing binding");
        // The Fragment can outlive this View; release the obsolete references.
        binding = null;
        super.onDestroyView();
    }

    /**
     * Displays the Fragment counter without changing it.
     * Call only between View creation and View destruction.
     */
    private void renderCount() {
        binding.fragmentValue.setText(getString(R.string.fragment_value, count));
    }
}
