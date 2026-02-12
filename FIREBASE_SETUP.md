# Firebase Setup Guide for ShopKeeper Pro

This guide will walk you through setting up Firebase for your ShopKeeper Pro app with cloud sync capabilities.

## Prerequisites

- Google account
- Android Studio installed
- ShopKeeper Pro project cloned and building successfully

## Step 1: Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Create a project"
3. Enter project name: "ShopKeeper-Pro" (or your preferred name)
4. Disable Google Analytics (optional, can enable if needed)
5. Click "Create Project"

## Step 2: Add Android App to Firebase

1. In Firebase Console, click "Add app" → Android icon
2. Enter the following details:
   - **Package name**: `com.shopkeeper.pro` (must match exactly)
   - **App nickname**: ShopKeeper Pro (optional)
   - **Debug signing certificate**: (optional, for Google Sign-In)
3. Click "Register app"

## Step 3: Download and Add google-services.json

1. Download the `google-services.json` file
2. Replace the placeholder file at: `/app/google-services.json`
3. This file contains your Firebase configuration

## Step 4: Enable Required Firebase Services

### A. Enable Authentication
1. In
Password" sign-in method
3. Click "Save"

### B. Enable Firestore Database
1. Go to "Firestore Database" → "Create database"
2. Choose "Start in production mode"
3. Select your preferred location (closest to your users)
4. Click "Create"

### C. Set Firestore Security Rules
1. Go to "Firestore Database" → "Rules"
2. Replace with these rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Allow users to read/write their own user document
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }

    // Store-level access
    match /stores/{storeId} {
      // Allow authenticated users to read store data
      allow read: if request.auth != null;

      // Store collections
      match /items/{itemId} {
        allow read: if request.auth != null;
        allow write: if request.auth != null &&
          get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin';
      }

      match /sales/{saleId} {
        allow read: if request.auth != null;
        allow write: if request.auth != null;
      }

      match /expenses/{expenseId} {
        allow read: if request.auth != null;
        allow write: if request.auth != null;
      }

      match /daily_totals/{date} {
        allow read: if request.auth != null;
        allow write: if request.auth != null;
      }
    }
  }
}
```

3. Click "Publish"

## Step 5: Create Test Users

The app includes a "Create Demo Users" button that will create:
- **Admin**: admin@shopkeeperpr
o.com / admin123
- **User**: user1@shopkeeperpro.com / user1234

Or create your own users via Firebase Console:
1. Go to "Authentication" → "Users"
2. Click "Add user"
3. Enter email and password

## Step 6: Test the Integration

1. Build and run the app
2. Click "Create Demo Users" or use your created credentials
3. Login and verify:
   - User authentication works
   - Data syncs to Firestore (check Firebase Console)
   - Offline mode works (turn off internet, make changes, turn on - should sync)

## Features Enabled

✅ **Cloud Sync**: All data automatically syncs to Firebase
✅ **Multi-device**: Login on multiple devices with same account
✅ **Offline Support**: Works offline, syncs when connected
✅ **Real-time Updates**: See changes from other devices instantly
✅ **Backup**: Data is backed up in the cloud
✅ **Role-based Access**: Admin vs User permissions enforced

## Architecture

```
┌─────────────┐     ┌──────────────┐     ┌──────────────┐
│   Android   │────▶│     Room     │────▶│   Firebase   │
│     App     │     │   Database   │     │  Firestore   │
│             │◀────│   (Local)    │◀────│   (Cloud)    │
└─────────────┘     └──────────────┘     └──────────────┘
                           │                      │
                           └──────────────────────┘
                            Bi-directional Sync
```

## Monitoring & Analytics

1. **Firebase Console**: Monitor usage, users, and data
2. **Performance Monitoring**: (Optional) Add Firebase Performance SDK
3. **Crashlytics**: (Optional) Add Firebase Crashlytics for crash reporting

## Pricing

Firebase offers a generous free tier:
- **Authentication**: 10k verifications/month free
- **Firestore**:
  - 1GB storage
  - 10GB/month bandwidth
  - 50k reads/day, 20k writes/day
- Perfect for small-medium businesses

## Troubleshooting

### App crashes on launch
- Ensure `google-services.json` is in the correct location
- Check package name matches exactly: `com.shopkeeper.pro`

### Authentication fails
- Verify Email/Password auth is enabled in Firebase Console
- Check internet connectivity
- Ensure email format is valid

### Data not syncing
- Check Firestore rules allow access
- Verify internet connectivity
- Check Firebase Console for any errors

### Build errors
- Run `./gradlew clean build`
- Sync project with Gradle files
- Ensure all Firebase dependencies are added

## Next Steps

1. **Custom Domain**: Set up custom email domain for auth emails
2. **Backup Rules**: Configure automated Firestore backups
3. **Monitoring**: Set up alerts for usage limits
4. **Multi-store**: Implement store selection for chain businesses

## Support

- Firebase Documentation: https://firebase.google.com/docs
- ShopKeeper Pro Issues: Create issue in your repository
- Firebase Support: https://firebase.google.com/support