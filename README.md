# Lifecycle

A standalone Java/XML Android project for the Lifecycle and saved-state tutorial.
Package: `com.example.topics`.

Local history contains two commits on one branch:

1. **Enable View Binding in Empty Views Activity** — the runnable template with
   View Binding enabled and MainActivity converted to binding.
2. **Implement Lifecycle and saved-state tutorial** — the three Activity counters,
   SharedPreferences persistence, and the Fragment counter with detach/Back and
   binding cleanup.

Open this folder in Android Studio. The final tutorial is the current project state.
Use the Git Log to compare the second commit with its parent, or run
`git diff HEAD^ HEAD` to see all Lifecycle changes from the View Binding baseline.

Tutorial: [Lifecycle and saved state](https://xn--7dbdbn4b5c.xn--eebf2b.com/android/topics/01-lifecycle-state/).

## Try the experiment

1. Tap **Increment all three** twice and **Increment Fragment** once.
2. Rotate: Activity field becomes 0; Bundle and SharedPreferences remain 2;
   Fragment remains 1.
3. Tap **Destroy Fragment View**, then device Back: Fragment count remains 1.
   In Logcat, filter `LifecycleDemo` and compare Fragment instance numbers.
4. Force stop and launch again: only SharedPreferences retains 2.
5. **Reset counters** resets the three Activity counters and their persistent value.
   The Fragment is an independent counter.

The Empty Views template's dependencies, test source sets, themes, manifest,
backup configuration, and edge-to-edge insets are retained.
