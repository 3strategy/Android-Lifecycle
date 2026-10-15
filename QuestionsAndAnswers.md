# Questions and Answers

### Question: How does pressing Back reattach the fragment after tapping Destroy Fragment View when there is no explicit attach() call?

#### Answer: Back reverses the transaction recorded by FragmentManager

The key is `.addToBackStack("detached_view")` in `LifecycleFragment.onViewCreated()` in [`LifecycleFragment.java`](app/src/main/java/com/example/topics/LifecycleFragment.java):

```java
binding.detachView.setOnClickListener(v -> getParentFragmentManager()
        .beginTransaction()
        .detach(this)
        .addToBackStack("detached_view")
        .commit());
```

This listener asks the fragment's owning `FragmentManager` to detach this fragment and record the transaction on its back stack. `commit()` schedules the transaction to run. The string `"detached_view"` names the back-stack entry; it is not a method name or a callback.

When the user presses device Back, AndroidX's built-in Back handling pops the top fragment transaction. Popping reverses its operations: the reverse of `detach(this)` is to attach that fragment again. The manager performs that operation internally, so this project does not need its own Back listener or an explicit `attach()` call. See the Android documentation on [fragment back-stack handling](https://developer.android.com/guide/fragments/fragmentmanager) and [fragment transactions](https://developer.android.com/guide/fragments/transactions).

The experiment follows this sequence:

1. The fragment and its view are visible.
2. **Destroy Fragment View** runs the detach transaction. The view is destroyed, but the fragment object remains managed by `FragmentManager`.
3. `onDestroyView()` clears `binding`, releasing this fragment's references to the old view tree.
4. Back pops the transaction and reverses the detach.
5. `onCreateView()` creates a new view tree and binding. `onViewCreated()` reconnects the listeners and calls `renderCount()`.

The relevant cleanup in this project's `onDestroyView()` is:

```java
binding = null;
super.onDestroyView();
```

The new view is created in `onCreateView()`:

```java
binding = FragmentLifecycleBinding.inflate(inflater, container, false);
return binding.getRoot();
```

During a simple detach/Back cycle, with no activity recreation or process death, the same fragment object survives. Its `count` field therefore survives too. Clearing `binding` does not clear `count`; `renderCount()` displays that field through the new binding. The instance numbers logged by this class let you compare the fragment identity before and after the cycle.

If Android recreates the component, retaining the old Java object is no longer the explanation. This project also saves `count` in `onSaveInstanceState()` and reads it in `onViewCreated()` when a saved bundle is supplied.

Without `addToBackStack(...)`, Back would not undo this detach automatically. Explicit reattachment would then require another transaction using `attach(fragment)`. Also, the transaction operation `detach()` is distinct from the lifecycle callback `onDetach()`; destroying the fragment's view does not mean destroying the fragment object or disconnecting it from its activity. See [attaching and detaching fragments](https://developer.android.com/guide/fragments/transactions#attaching-and-detaching-fragments).

### Question: Is the fragment declaration in activity_main.xml enough to display it, or does MainActivity.java also add the fragment?

#### Answer: XML declares the fragment, and MainActivity inflates and displays the layout

There is no explicit fragment-add transaction in [`MainActivity.java`](app/src/main/java/com/example/topics/MainActivity.java). The declaration in [`activity_main.xml`](app/src/main/res/layout/activity_main.xml) selects the fragment class and its container:

```xml
<androidx.fragment.app.FragmentContainerView
    android:id="@+id/lifecycle_fragment"
    android:name="com.example.topics.LifecycleFragment"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginTop="32dp" />
```

`android:name` is the important part for automatic fragment creation. A `FragmentContainerView` without that attribute supplies a container, but does not select a fragment to instantiate. The ID identifies the container. AndroidX adds the declared fragment during layout inflation, or uses the fragment restored by its manager when applicable. See [adding a fragment using XML](https://developer.android.com/guide/fragments/create#add-xml).

The XML still needs to be loaded by a fragment-capable activity. This project's activity extends `AppCompatActivity`, which inherits AndroidX fragment support, and its `onCreate()` calls:

```java
super.onCreate(savedInstanceState);
EdgeToEdge.enable(this);
binding = ActivityMainBinding.inflate(getLayoutInflater());
setContentView(binding.getRoot());
```

Here, `ActivityMainBinding` is generated from `activity_main.xml` because View Binding is enabled in [`app/build.gradle.kts`](app/build.gradle.kts). `inflate(...)` builds that layout, including the `FragmentContainerView`. `setContentView(...)` installs the resulting root as the activity's screen. The superclass supplies the fragment infrastructure and restoration handling.

The fragment then supplies its own UI through `LifecycleFragment.onCreateView()`. That method inflates [`fragment_lifecycle.xml`](app/src/main/res/layout/fragment_lifecycle.xml) using `FragmentLifecycleBinding` and returns the root. The `false` argument to its `inflate(...)` call leaves attachment of that root to `FragmentManager`.

So the XML declaration is sufficient to specify and automatically add this fragment within the existing activity setup. The Java code is still necessary to host fragments and load/display the activity layout, but it does not need an additional `add()` or `attach()` transaction. The counter listeners, edge-to-edge setup, and window-inset listener do not select or add the fragment.

If you removed `android:name`, you would need to add the fragment through code instead. That alternative should normally add it only on a fresh creation (`savedInstanceState == null`), allowing the manager to restore it on recreation. It is an alternative to this project's XML declaration, not an extra step required by the current code.

### Question: Who calls onSaveInstanceState(), and how is the Bundle filled, saved, and delivered when the activity or fragment is recreated?

#### Answer: Android requests a snapshot, your code fills it, and Android supplies it during restoration

Android calls the activity's `onSaveInstanceState(Bundle outState)` through its lifecycle machinery. You override this callback to contribute your state; the button listeners do not call it. Android supplies `outState`, your method writes values into it, and Android retains the resulting saved state for possible recreation. See [saving activity instance state](https://developer.android.com/guide/components/activities/activity-lifecycle#save-simple-lightweight-ui-state-using-onsaveinstancestate).

In [`MainActivity.java`](app/src/main/java/com/example/topics/MainActivity.java), the callback is:

```java
@Override
protected void onSaveInstanceState(Bundle outState) {
    outState.putInt(SAVED_COUNT, savedCount);
    super.onSaveInstanceState(outState);
}
```

`SAVED_COUNT` is the key `"saved_count"`. If `savedCount` is 2 when the callback runs, `putInt(...)` puts that number into the supplied bundle under that key. It does not save the activity object, the binding, or every field automatically.

`super.onSaveInstanceState(outState)` lets the inherited Android/AndroidX implementation contribute framework state, including supported view state and fragment-manager state. It is part of completing the save operation, rather than calling your override again.

For this project's Android versions (minimum API 31), a normal rotation that recreates the activity follows this activity callback sequence:

```text
Old MainActivity:
    onPause()
    onStop()
    onSaveInstanceState(outState)   <-- copy savedCount into the snapshot
    onDestroy()

New MainActivity:
    onCreate(savedInstanceState)   <-- read savedCount from the snapshot
    onStart()
    onRestoreInstanceState(...)    <-- inherited restoration; no override here
    onResume()
```

For apps targeting API 28 or later, `onSaveInstanceState()` occurs after `onStop()` when it is invoked. Older targeting rules differ. This is a rotation example, not a promise that every pause, stop, or destruction invokes saving. See the [Activity callback reference](https://developer.android.com/reference/android/app/Activity#onSaveInstanceState(android.os.Bundle)).

The new activity reads the saved value in this project's `onCreate()`:

```java
if (savedInstanceState != null) {
    savedCount = savedInstanceState.getInt(SAVED_COUNT);
}
```

`outState` means the outgoing snapshot being filled; `savedInstanceState` means the incoming saved state being read. These are parameter names, not two storage services. Restoration preserves the saved values; it does not require the exact same Java `Bundle` object to survive.

That explains this project's rotation result after two increments: `memoryCount` starts at 0 in the new activity because the code never saves it, `savedCount` becomes 2 from the bundle, and `persistentCount` becomes 2 by reading `SharedPreferences`. `renderCounts()` then displays those fields.

The fragment's snapshot is coordinated by AndroidX `FragmentManager` as part of saving the host activity. It calls the fragment's `onSaveInstanceState()` and includes that fragment's saved state within the manager's state. The activity and fragment receive their own state bundles; they do not share one flat collection of custom keys. See [saving state with fragments](https://developer.android.com/guide/fragments/saving-state#savedstate).

In [`LifecycleFragment.java`](app/src/main/java/com/example/topics/LifecycleFragment.java), the fragment contributes its own counter:

```java
@Override
public void onSaveInstanceState(@NonNull Bundle outState) {
    outState.putInt(SAVED_COUNT, count);
    super.onSaveInstanceState(outState);
}
```

Here, `SAVED_COUNT` means `"fragment_count"`. On restoration, the manager supplies this fragment's saved bundle to its creation callbacks. This project reads the counter in `onViewCreated()`:

```java
if (savedInstanceState != null) {
    count = savedInstanceState.getInt(SAVED_COUNT);
}
```

The activity does not manually call the fragment's save method or forward its own bundle to the fragment. Also, the earlier detach/Back experiment can retain `count` in the surviving fragment object without needing a fresh saved-state callback just because its view was destroyed.

The bundle is a small restoration snapshot, not a file your code writes or permanent app storage. Android keeps saved state outside the app process so it can restore a retained task after system-initiated process death. The snapshot must already have been collected; killing a process does not give the app a final opportunity to run this callback. Finishing the activity, force stopping, or dismissing the task is not a durable-storage use case. Use persistent storage for values that must survive those actions. See [saved state and persistence](https://developer.android.com/topic/libraries/architecture/saving-states).

In this demo, the persistent counter is written separately on every increment:

```java
preferences.edit().putInt(PERSISTENT_COUNT, persistentCount).apply();
```

So there are two distinct triggers in the code: an increment changes the fields and requests a preferences write immediately; an Android save request copies the current `savedCount` and fragment `count` into restoration bundles later.
