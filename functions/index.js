const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.onEmergencyTriggered = functions.firestore
  .document("emergencies/{emergencyId}")
  .onCreate(async (snapshot, context) => {
    const emergencyData = snapshot.data();
    const victimId = emergencyData.victimUserId;

    // Fetch user details & emergency contacts
    const userDoc = await admin.firestore().doc(`users/${victimId}`).get();
    const userData = userDoc.data() || { fullName: "EchoSafe User" };

    const contactsSnapshot = await admin
      .firestore()
      .collection(`users/${victimId}/contacts`)
      .get();

    const tokens = [];
    contactsSnapshot.forEach((doc) => {
      const data = doc.data();
      if (data.fcmToken) {
        tokens.push(data.fcmToken);
      }
    });

    if (tokens.length === 0) return null;

    const lat = emergencyData.currentLocation ? emergencyData.currentLocation.latitude : 0.0;
    const lng = emergencyData.currentLocation ? emergencyData.currentLocation.longitude : 0.0;

    // High-Priority FCM Payload (Overrides Do-Not-Disturb on receiver)
    const payload = {
      notification: {
        title: `EMERGENCY ALERT: ${userData.fullName}`,
        body: `Danger detected! Location: https://maps.google.com/?q=${lat},${lng}`,
      },
      data: {
        emergencyId: context.params.emergencyId,
        highPriority: "true",
        latitude: String(lat),
        longitude: String(lng),
        triggerType: emergencyData.triggerType || "PROLONGED_SCREAM",
      },
      android: {
        priority: "high",
        notification: {
          channelId: "emergency_dnd_override",
          sound: "alarm_sound.mp3",
        },
      },
    };

    return admin.messaging().sendMulticast({
      tokens: tokens,
      ...payload,
    });
  });
