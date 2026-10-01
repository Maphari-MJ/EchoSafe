# EchoSafe — Backend Agent Specification & Implementation Guide

> **For Backend AI Agent / Developer:** Use this document to build, update, or verify the Node.js/Express + MongoDB backend server running on your machine. This guide contains full route specs, database schemas, and Ngrok configuration to ensure 100% feature parity with the EchoSafe Android Kotlin Mobile Application.

---

## 1. Environment & Server Setup

### Technology Stack
- **Runtime:** Node.js (v18+) with Express.js (TypeScript or JavaScript)
- **Database:** MongoDB (Mongoose ORM)
- **Authentication:** JSON Web Tokens (JWT) + bcryptjs
- **Tunneling:** Ngrok (`ngrok http 5000`)

### Required Package Dependencies (`package.json`)
```json
{
  "name": "echosafe-backend",
  "version": "1.2.0",
  "main": "server.js",
  "scripts": {
    "start": "node server.js",
    "dev": "nodemon server.js"
  },
  "dependencies": {
    "express": "^4.19.2",
    "mongoose": "^8.3.0",
    "jsonwebtoken": "^9.0.2",
    "bcryptjs": "^2.4.3",
    "cors": "^2.8.5",
    "dotenv": "^16.4.5",
    "multer": "^1.4.5-lts.1"
  },
  "devDependencies": {
    "nodemon": "^3.1.0"
  }
}
```

---

## 2. Express Server Setup & Ngrok Bypass Middleware (`server.js`)

Ensure your Express server handles CORS and Ngrok headers:

```javascript
const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
require('dotenv').config();

const app = express();

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Bypass Ngrok Browser Warning Header Interceptor
app.use((req, res, next) => {
  res.setHeader('ngrok-skip-browser-warning', 'true');
  next();
});

// Database Connection
mongoose.connect(process.env.MONGODB_URI || 'mongodb://localhost:27017/echosafe')
  .then(() => console.log('MongoDB connected successfully.'))
  .catch(err => console.error('MongoDB connection error:', err));

// Routes
app.use('/api/auth', require('./routes/auth'));
app.use('/api/contacts', require('./routes/contacts'));
app.use('/api/calibration', require('./routes/calibration'));
app.use('/api/emergencies', require('./routes/emergencies'));
app.use('/api/evidence', require('./routes/evidence'));
app.use('/api/hotspots', require('./routes/hotspots'));
app.use('/api/users', require('./routes/users'));

app.get('/api/health', (req, res) => {
  res.json({ status: 'OK', uptime: process.uptime(), timestamp: new Date().toISOString() });
});

const PORT = process.env.PORT || 5000;
app.listen(PORT, '0.0.0.0', () => {
  console.log(`EchoSafe Backend running on port ${PORT}`);
});
```

---

## 3. MongoDB Schemas & Data Models

### 3.1 User Model (`models/User.js`)
```javascript
const mongoose = require('mongoose');

const UserSchema = new mongoose.Schema({
  fullName: { type: String, required: true },
  email: { type: String, required: true, unique: true },
  password: { type: String, required: true },
  age: { type: Number, default: 21 },
  gender: { type: String, default: 'Prefer not to say' },
  fcmToken: { type: String, default: '' },
  secretKeyphraseHash: { type: String, default: '' },
  safeWordHash: { type: String, default: '' },
  createdAt: { type: Date, default: Date.now }
});

module.exports = mongoose.model('User', UserSchema);
```

### 3.2 Contact Model (`models/Contact.js`)
```javascript
const mongoose = require('mongoose');

const ContactSchema = new mongoose.Schema({
  userId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  contactName: { type: String, required: true },
  phoneNumber: { type: String, required: true },
  relationship: { type: String, default: 'Friend' },
  hasDndPermission: { type: Boolean, default: true },
  createdAt: { type: Date, default: Date.now }
});

module.exports = mongoose.model('Contact', ContactSchema);
```

### 3.3 Voice Calibration Model (`models/Calibration.js`)
```javascript
const mongoose = require('mongoose');

const CalibrationSchema = new mongoose.Schema({
  userId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true, unique: true },
  baselineRMS: { type: Number, default: 45.0 },
  distressPitchThreshold: { type: Number, default: 550.0 },
  secretKeyphraseHash: { type: String, default: '' },
  safeWordHash: { type: String, default: '' },
  lastCalibratedAt: { type: Date, default: Date.now }
});

module.exports = mongoose.model('Calibration', CalibrationSchema);
```

### 3.4 Emergency Event Model (`models/Emergency.js`)
```javascript
const mongoose = require('mongoose');

const EmergencySchema = new mongoose.Schema({
  victimUserId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  timestamp: { type: Date, default: Date.now },
  status: { type: String, enum: ['ACTIVE', 'RESOLVED', 'CANCELLED'], default: 'ACTIVE' },
  triggerType: { 
    type: String, 
    enum: ['PROLONGED_SCREAM', 'SECRET_KEYPHRASE', 'HARDWARE_DOUBLE_TAP', 'MANUAL_SOS'], 
    default: 'PROLONGED_SCREAM' 
  },
  currentLocation: {
    latitude: { type: Number, required: true },
    longitude: { type: Number, required: true }
  },
  locationAccuracy: { type: Number, default: 5.0 },
  locationBreadcrumbs: [{
    latitude: Number,
    longitude: Number,
    timestamp: { type: Date, default: Date.now }
  }],
  resolvedAt: { type: Date },
  disarmedAt: { type: Date }
});

module.exports = mongoose.model('Emergency', EmergencySchema);
```

### 3.5 Evidence Vault Item Model (`models/Evidence.js`)
```javascript
const mongoose = require('mongoose');

const EvidenceSchema = new mongoose.Schema({
  emergencyId: { type: String, required: true },
  userId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  mediaType: { 
    type: String, 
    enum: ['AUDIO_CLIP', 'CAMERA_SNAPSHOT', 'GPS_BREADCRUMB'], 
    required: true 
  },
  storageUrl: { type: String, default: '' },
  localFilePath: { type: String, default: '' },
  sha256Hash: { type: String, required: true },
  fileSizeBytes: { type: Number, default: 0 },
  originalFileName: { type: String },
  capturedAt: { type: Date, default: Date.now },
  isTamperProof: { type: Boolean, default: true }
});

module.exports = mongoose.model('Evidence', EvidenceSchema);
```

### 3.6 Hotspot Model (`models/Hotspot.js`)
```javascript
const mongoose = require('mongoose');

const HotspotSchema = new mongoose.Schema({
  reportedBy: { type: String, default: 'Anonymized User' },
  riskLevel: { type: String, enum: ['HIGH', 'MODERATE'], default: 'HIGH' },
  description: { type: String, required: true },
  location: {
    latitude: { type: Number, required: true },
    longitude: { type: Number, required: true }
  },
  radiusMeters: { type: Number, default: 500.0 },
  createdAt: { type: Date, default: Date.now }
});

module.exports = mongoose.model('Hotspot', HotspotSchema);
```

---

## 4. Complete REST API Endpoints Specification

### 4.1 Authentication Routes (`/api/auth`)
- **`POST /api/auth/register`**
  - **Body:** `{ "fullName": "Jane Doe", "email": "jane@example.com", "password": "Pass", "age": 22, "gender": "Female" }`
  - **Response:** `{ "success": true, "token": "JWT_TOKEN", "user": { "id": "...", "fullName": "Jane Doe", "email": "..." } }`

- **`POST /api/auth/login`**
  - **Body:** `{ "email": "jane@example.com", "password": "Pass" }`
  - **Response:** `{ "success": true, "token": "JWT_TOKEN", "user": { ... } }`

- **`GET /api/auth/profile`** [Auth Header Required]
  - **Response:** `{ "success": true, "user": { ... } }`

- **`POST /api/auth/send-otp`** (NEW Endpoint)
  - **Body:** `{ "email": "jane@example.com" }`
  - **Response:** `{ "success": true, "message": "OTP sent to email." }`

- **`POST /api/auth/verify-otp`** (NEW Endpoint)
  - **Body:** `{ "email": "jane@example.com", "otp": "123456" }`
  - **Response:** `{ "success": true, "verified": true }`

---

### 4.2 Emergency Contacts Routes (`/api/contacts`)
- **`GET /api/contacts`** [Auth Required]
  - **Response:** `{ "success": true, "data": [ { "id": "...", "contactName": "Parent", "phoneNumber": "+27821112233", "relationship": "Parent", "hasDndPermission": true } ] }`

- **`POST /api/contacts`** [Auth Required]
  - **Body:** `{ "contactName": "Trusted Friend", "phoneNumber": "+27824445566", "relationship": "Friend", "hasDndPermission": true }`
  - **Response:** `{ "success": true, "contact": { ... } }`

- **`POST /api/contacts/batch`** (NEW Endpoint) [Auth Required]
  - **Body:** `{ "contacts": [ { "contactName": "Contact 1", "phoneNumber": "+27820000001" }, ... ] }`
  - **Response:** `{ "success": true, "count": 3 }`

- **`PUT /api/contacts/:id`** (NEW Endpoint) [Auth Required]
  - **Body:** `{ "phoneNumber": "+27829990000", "hasDndPermission": true }`
  - **Response:** `{ "success": true, "contact": { ... } }`

- **`DELETE /api/contacts/:id`** [Auth Required]
  - **Response:** `{ "success": true, "message": "Contact deleted" }`

---

### 4.3 Voice Calibration Routes (`/api/calibration`)
- **`GET /api/calibration`** [Auth Required]
  - **Response:** `{ "success": true, "calibration": { "baselineRMS": 45.0, "distressPitchThreshold": 550.0 } }`

- **`POST /api/calibration`** [Auth Required]
  - **Body:** `{ "baselineRMS": 45.0, "distressPitchThreshold": 550.0, "secretKeyphrase": "help me", "safeWord": "safe disarm" }`
  - **Response:** `{ "success": true, "calibration": { ... } }`

- **`POST /api/calibration/verify-safeword`** [Auth Required]
  - **Body:** `{ "safeWord": "safe disarm" }`
  - **Response:** `{ "success": true, "isValid": true }`

---

### 4.4 Emergency SOS Routes (`/api/emergencies`)
- **`POST /api/emergencies/trigger`** [Auth Required]
  - **Body:** `{ "triggerType": "PROLONGED_SCREAM", "currentLocation": { "latitude": -26.2041, "longitude": 28.0473 }, "locationAccuracy": 5.0 }`
  - **Response:** `{ "success": true, "emergency": { "id": "...", "status": "ACTIVE", "triggerType": "PROLONGED_SCREAM" } }`

- **`POST /api/emergencies/:id/location`** (NEW Live Location Endpoint) [Auth Required]
  - **Body:** `{ "latitude": -26.2045, "longitude": 28.0478, "accuracy": 4.5 }`
  - **Response:** `{ "success": true, "locationBreadcrumbsCount": 4 }`

- **`POST /api/emergencies/:id/disarm`** [Auth Required]
  - **Body:** `{ "safeWord": "safe disarm", "disarmReason": "USER_DISARMED" }`
  - **Response:** `{ "success": true, "emergency": { "status": "CANCELLED" } }`

- **`POST /api/emergencies/:id/resolve`** [Auth Required]
  - **Response:** `{ "success": true, "emergency": { "status": "RESOLVED" } }`

- **`GET /api/emergencies/active`** [Auth Required]
  - **Response:** `{ "success": true, "emergencies": [ ... ] }`

- **`GET /api/emergencies/:id`** [Auth Required]
  - **Response:** `{ "success": true, "emergency": { ... } }`

---

### 4.5 Evidentiary Vault Routes (`/api/evidence`)
- **`POST /api/evidence/upload`** (Multipart Form Data) [Auth Required]
  - **Form Fields:** `emergencyId` (text), `mediaType` (text: AUDIO_CLIP/CAMERA_SNAPSHOT/GPS_BREADCRUMB), `file` (binary stream)
  - **Response:** `{ "success": true, "evidence": { "id": "...", "sha256Hash": "a3f5...", "storageUrl": "..." } }`

- **`POST /api/evidence/:id/verify`** [Auth Required]
  - **Response:** `{ "success": true, "isTamperProof": true, "currentHash": "a3f5...", "storedHash": "a3f5..." }`

- **`GET /api/evidence/user`** [Auth Required]
  - **Response:** `{ "success": true, "items": [ ... ] }`

- **`GET /api/evidence/:id/download`** (NEW Raw File Stream Endpoint) [Auth Required]
  - **Response:** Binary Stream download (`Content-Type: audio/pcm` or `image/jpeg`)

---

### 4.6 Hotspots & FCM Token Routes
- **`GET /api/hotspots`**
  - **Query Params:** `latitude`, `longitude`, `radiusMeters`
  - **Response:** `{ "success": true, "hotspots": [ { "id": "...", "riskLevel": "HIGH", "description": "Dim underpass", "location": { "latitude": -26.2041, "longitude": 28.0473 } } ] }`

- **`POST /api/hotspots`** [Auth Required]
  - **Body:** `{ "riskLevel": "HIGH", "description": "Harassment incident logged", "location": { "latitude": -26.2041, "longitude": 28.0473 } }`
  - **Response:** `{ "success": true, "hotspot": { ... } }`

- **`POST /api/users/fcm-token`** (NEW FCM Sync Endpoint) [Auth Required]
  - **Body:** `{ "fcmToken": "fcm_device_token_string..." }`
  - **Response:** `{ "success": true }`

---

## 5. Exposing Backend via Ngrok

Once your server is running locally on port 5000:
1. Open a terminal on your backend machine and run:
   ```bash
   ngrok http 5000
   ```
2. Ngrok will output a HTTPS URL, for example:
   `https://a1b2-102-132-21-4.ngrok-free.app`
3. Send this generated Ngrok URL to the Android team so they can set it as the active `BASE_URL` in the mobile app.
