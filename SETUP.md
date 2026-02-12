# Setup Instructions

## Firebase Configuration

This app requires a `google-services.json` file from Firebase to run.

### Steps:
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select or create your Firebase project
3. Add an Android app with package name: `com.shopkeeper.pro`
4. Download the `google-services.json` file
5. Place it in the `app/` directory

**IMPORTANT:** Never commit `google-services.json` to version control. It contains sensitive API keys.

The file is already listed in `.gitignore` to prevent accidental commits.