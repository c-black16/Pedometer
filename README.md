# Glass Pedometer
A tiny Android step-counter with a glass-inspired interface and a large **Pedometer** heading.

## Build APK with GitHub
1. Create a new **private** GitHub repository with the default branch named `main`.
2. Extract this ZIP. Upload **all extracted contents** (including `.github` folder) to the root of your repository, not the enclosing `PedometerProject` folder. GitHub web upload may hide dotfolders in some file pickers; verify `.github/workflows/build.yml` appears in the repository.
3. Open the **Actions** tab. Choose **Build Pedometer APK**, then **Run workflow** if it did not start after your commit.
4. Open the successful run and download **Pedometer-APK** from Artifacts. Unzip it to obtain `app-debug.apk`.
5. On your Android phone, open the APK and allow installation from your browser/files app if prompted.

## Important limitation
The Android step counter gives a cumulative count since boot. This simple app sets the baseline at its **first sensor reading each day**. Steps before that reading may not be included, especially after midnight or a reboot. When you reopen the app, it catches up from the sensor's cumulative reading for the current baseline. It does not run a background service or continuously track midnight boundaries. This is a minimal personal prototype, not a validated health tracker.

## Privacy
No network permission, ads, analytics, or account. Step readings are saved only in local app preferences.
