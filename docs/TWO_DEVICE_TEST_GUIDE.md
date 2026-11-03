# Real-Device Testing Guide (Two Android Phones)

This guide walks through verifying DirectShare on **Phone A (Sender)** and **Phone B (Receiver)**.

---

## Prerequisite Checklist
- Two physical Android devices running Android 8.0 (API 26) or higher.
- `app/google-services.json` configured for your Firebase project.
- Anonymous Auth enabled and `database.rules.json` published in Firebase Console.
- Install the debug build on both phones:
  ```bash
  ./gradlew installDebug
  ```

---

## Scenario 1: Both Phones on the Same Wi-Fi Network
1. Open DirectShare on **Phone A** and **Phone B** and complete the Onboarding screen.
2. Verify both phones show the green **Signaling Online** pill at the top of the Home screen.
3. On **Phone B (Receiver)**, tap **Receive Files → Host Receive Session**.
   - Phone B displays a 6-character pairing code (e.g., `K7M9P2`) and a **Show QR Code** button.
4. On **Phone A (Sender)**, tap **Connect / Pair Device**, enter Phone B's 6-character code, and tap **Connect with Code** (or tap **Connect** next to Phone B under *Online Peers Registered on Signaling*).
5. **Explicit Pairing Authorization**:
   - Phone B displays an **Approve Pairing Request** card showing Phone A's device name.
   - Tap **Authorize** on Phone B.
6. **WebRTC Negotiation**:
   - Both phones transition to `Connecting...` while exchanging SDP Offer/Answer and ICE candidates, then show **Connected to [Peer Name] (WebRTC P2P Channel Open)**.

---

## Scenario 2: Receiver Rejection vs. Receiver Acceptance
1. On **Phone A**, go to **Send Files** and pick 2 images via the **Images** chip.
2. Tap **Send** and confirm the recipient in the confirmation dialog.
3. **Test Rejection**:
   - On **Phone B**, the **Incoming Transfer Request** screen appears with Phone A's name, file names, and total size.
   - Tap **Decline** on Phone B.
   - Verify Phone A immediately shows **Transfer Failed: Receiver Declined Transfer** and both phones record the `REJECTED` status in **Transfer History**.
4. **Test Acceptance**:
   - Tap **Back / Retry** on Phone A, tap **Send** again, and tap **Accept** on Phone B.
   - Verify both phones display live progress gauges, byte counters, transfer speeds, and ETAs.
   - When complete, verify **SHA-256 Checksum Verified** appears on Phone B and test the **Open** and **Share** buttons.

---

## Scenario 3: Small Files, 0-Byte Files, and Large Files (500 MB+)
1. Select a mix of small documents (`.pdf`, `.txt`), an `.apk` or `.zip`, and a large video file (`500 MB+`).
2. Start the transfer and open **Connection Diagnostics** or watch the live transfer card.
3. Verify that memory usage remains flat (chunks are streamed in 64 KB increments via `ContentResolver`) and that `bufferedAmount` backpressure smoothly paces the sender when the data channel buffer fills.
4. Check the Firebase Realtime Database console during the transfer to confirm `/signalingSessions/{sessionId}` was already cleaned up when the DataChannel opened and **zero file bytes** pass through Firebase.

---

## Scenario 4: Network Interruption, Cancellation & Temp File Cleanup
1. Start sending a large video file (`200 MB+`).
2. At ~40% progress, toggle **Airplane Mode** or disable Wi-Fi on one phone (or tap **Cancel Transfer**).
3. Verify:
   - The active transfer stops immediately and transitions to **Transfer Failed (Peer-to-Peer Connection Interrupted)** or **Transfer Cancelled**.
   - All partial `.tmp` files in `cacheDir/transfers/` on the receiving phone are deleted automatically.
   - The UI clearly states that incomplete files were cleaned up and the transfer must be restarted after reconnecting.

---

## Scenario 5: Different Wi-Fi Networks & Mobile Data (STUN vs. TURN)
1. Connect **Phone A** to Wi-Fi Network 1 and **Phone B** to Wi-Fi Network 2 (or 4G/5G Mobile Data).
2. Pair both phones using the 6-digit pairing code.
3. **Expected Behavior**:
   - If both routers/carriers use Full-Cone or Restricted-Cone NAT, STUN hole-punching succeeds and the transfer proceeds directly peer-to-peer.
   - If one or both networks use strict **Symmetric NAT / CGNAT**, ICE checking transitions to `FAILED` and DirectShare displays:
     `NAT/Firewall Traversal Failed (TURN Required)`
   - To test across strict Symmetric NATs/CGNAT, open **Settings → Optional TURN Relay Fallback** on both phones, enter your TURN server (`turn:your-turn-server:3478`, username, and credential), save, and reconnect.

---

## Scenario 6: Firebase Rules & Unauthorized Access Verification
1. Attempt to join a session using a random/non-existent 6-character code → Verify immediate `Pairing code not found or already expired` error.
2. Wait 10 minutes after creating a session without connecting → Verify the session expires (`SESSION_TTL_MS = 10 minutes`) and rejects join attempts.
3. In Firebase Console → **Realtime Database → Rules Playground**, simulate:
   - Unauthenticated read/write (`auth == null`) on `/signalingSessions/testId` → **Denied**.
   - Authenticated user `uid_C` attempting to write to `/signalingSessions/testId/offer` where `hostUid == uid_A` → **Denied**.
