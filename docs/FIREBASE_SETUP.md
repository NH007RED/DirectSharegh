# Firebase Configuration & Security Rules Setup Guide

Follow these steps to connect DirectShare to your own Firebase project before building and testing on two Android phones.

## Step 1: Create the Firebase Project
1. Go to [https://console.firebase.google.com/](https://console.firebase.google.com/) and click **Create a project**.
2. Enter a project name (e.g., `directshare-webrtc`) and complete creation.

## Step 2: Register the Android App
1. In Project Overview, click the **Android** icon to add an Android app.
2. Enter the exact package name:
   ```text
   com.directshare.app
   ```
3. Download the generated `google-services.json` file.
4. Copy `google-services.json` into `DirectShare/app/google-services.json` (replacing the placeholder template file).

## Step 3: Enable Firebase Authentication (Anonymous Sign-In)
1. In the left sidebar, open **Build → Authentication**.
2. Click **Get started**, go to the **Sign-in method** tab, select **Anonymous**, toggle **Enable**, and click **Save**.
3. Every device running DirectShare authenticates with a unique Firebase Auth `uid` before accessing any signaling path.

## Step 4: Create Firebase Realtime Database & Publish Security Rules
1. In the left sidebar, open **Build → Realtime Database** and click **Create Database**.
2. Choose your closest region and start in **Locked mode**.
3. Open the **Rules** tab and replace the rules with the exact contents of `database.rules.json` in the project root.
4. Click **Publish**.

### What `database.rules.json` Enforces:
- **Zero Public Access**: `.read` and `.write` are `false` at the root.
- **Participant Validation**:
  - Only the session's `hostUid` can write `/signalingSessions/$sessionId/offer` or toggle `participants/authorized`.
  - Only the session's `guestUid` can write `/signalingSessions/$sessionId/answer`.
  - Only `hostUid` or `guestUid` can write under `/signalingSessions/$sessionId/iceCandidates/$uid`, and only to their own `$uid` branch.
- **Payload Size & Schema Validation**:
  - SDP strings are capped at `16384` characters and ICE candidate strings are capped at `2048` characters, preventing anyone from abusing signaling nodes to store file chunks.
  - Sessions require a valid `expiresAt` timestamp in the future (`newData.child('expiresAt').val() > now`).
