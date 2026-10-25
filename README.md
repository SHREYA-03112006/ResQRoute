#  ResQRoute

### Intelligent Disaster Monitoring & Emergency Response Routing Platform

> **Detect Faster. Respond Smarter. Save Lives.**

ResQRoute is an intelligent emergency-response platform designed to help rescue teams identify disaster-affected areas, collect real-time disaster intelligence from online sources, visualize incidents on a city-wise map, track rescue-team locations, recommend appropriate emergency resources, and plan suitable response routes.

The platform combines **Google Maps**, **live device location**, **TinyFish web intelligence**, disaster-report processing, incident prioritization, resource recommendation, and dispatcher-controlled rescue planning into a unified dashboard.

---

## 🎯 Problem Statement

During disasters such as fires, floods, landslides, and road blockages, emergency teams often face several challenges:

- Disaster information is scattered across different sources.
- It is difficult to identify the exact affected area quickly.
- Online reports may contain incomplete or conflicting information.
- Rescue teams need to know which incidents should be prioritized.
- Different disasters require different emergency resources.
- Normal map routes may not account for reported road blockages.
- Rescue teams need location-aware route planning.
- Emergency decisions must be made quickly while maintaining human control.

ResQRoute aims to bring these pieces together into one intelligent emergency-response platform.

---

# 💡 Solution

ResQRoute provides a three-phase emergency-response workflow:

```text
City & Live Location
        ↓
Disaster Intelligence
        ↓
Rescue Planning
        ↓
Dispatcher Decision
        ↓
Emergency Response
```

### Phase 1 — City-Wise Map & Live Location

Provides:

- City-wise interactive map
- Google Maps integration
- City selection
- Disaster/incident markers
- Affected-area visualization
- Rescue-team device location
- Start/stop location tracking
- Map legends
- Location permission handling

### Phase 2 — TinyFish Disaster Intelligence

Provides:

- Web-based disaster information collection
- Disaster report extraction
- Fire, flood, landslide and road-blockage detection
- Location and road matching
- Source and timestamp preservation
- Report confidence
- Duplicate detection
- Unverified-report handling
- Live/demo data distinction

### Phase 3 — Rescue Planning

Provides:

- Incident prioritization
- Disaster-specific resource recommendations
- Route planning
- Road-blockage warnings
- Rescue-team assignment workflow
- Incident status tracking
- Dispatcher approval
- Final emergency-response dashboard

---

# ✨ Key Features

## 🗺️ City-Wise Interactive Map

The system provides a city-level map where users can:

- Select a city
- View roads and important locations
- View reported incidents
- View affected areas
- View rescue-team location
- View planned response routes

Google Maps is used for map visualization and route planning.

---

## 📍 Live Rescue-Team Location

ResQRoute uses the browser's Geolocation API to obtain the current device location after user permission.

The rescue team can:

- Start location tracking
- View current location
- Monitor movement on the map
- Stop tracking when required

The system does not secretly track users.

---

## 🚨 Disaster Intelligence

ResQRoute integrates TinyFish to collect information from relevant web sources.

The system can process information related to:

- 🔥 Fire
- 🌊 Flood
- 🏔️ Landslide
- 🚧 Road blockage
- 🏢 Building collapse
- 🏥 Medical emergencies
- ⚠️ Other emergency situations

The extracted information is converted into structured incident records.

---

## 🔎 Disaster Report Processing

Each incident can contain:

| Information | Description |
|---|---|
| Title | Incident title |
| Category | Fire, flood, landslide, etc. |
| Location | Reported location |
| Road | Associated road when available |
| Coordinates | Latitude/longitude when reliably available |
| Source | Original information source |
| Source URL | Original report URL |
| Published Time | When the report was published |
| Retrieved Time | When ResQRoute collected it |
| Confidence | Reliability of extracted information |
| Status | Verification/processing state |

The system does not fabricate missing information.

---

# 🧠 Incident Prioritization

Incidents are prioritized using available information such as:

- Reported severity
- Injuries
- Trapped people
- Affected population when known
- Source reliability
- Report freshness
- Location confidence

Possible priority levels:

```text
CRITICAL
HIGH
MEDIUM
LOW
```

The priority system assists dispatchers and does not replace human decision-making.

---

# 🚒 Emergency Resource Recommendation

ResQRoute recommends resources according to the detected disaster.

### 🔥 Fire

Possible recommendations:

- Fire brigade
- Firefighting equipment
- Fire-response personnel

### 🌊 Flood

Possible recommendations:

- Rescue boats
- Life jackets
- Evacuation teams
- Emergency shelters

### 🏔️ Landslide

Possible recommendations:

- Search-and-rescue teams
- Medical support
- Clearance equipment

### 🏢 Building Collapse

Possible recommendations:

- Search-and-rescue personnel
- Structural specialists
- Ambulances
- Medical supplies

### 🚧 Road Blockage

Possible recommendations:

- Road clearance teams
- Alternative route assessment

Recommendations are intended to support dispatcher decisions.

---

# 🛣️ Route Planning

ResQRoute integrates map-based route planning to help determine a suitable route between the rescue team and the selected incident.

The system can display:

- Route
- Distance
- Estimated travel time
- Alternative routes where available
- Road-blockage warnings

### Important limitation

Google Maps does not automatically guarantee that every disaster or road blockage is known.

Therefore, ResQRoute does **not** automatically claim that a route is disaster-safe.

If a road blockage is reported but cannot be reliably integrated into route calculation, the system warns the dispatcher instead of falsely claiming that the route is safe.

---

# 👨‍🚒 Rescue Assignment

The dispatcher can review an incident and:

1. Review incident information.
2. Check source and confidence.
3. Review priority.
4. Review recommended resources.
5. Select an available rescue team/resource when actual data exists.
6. Review the proposed route.
7. Approve the assignment.
8. Track the incident status.

Example incident lifecycle:

```text
REPORTED
   ↓
UNDER REVIEW
   ↓
APPROVED
   ↓
ASSIGNED
   ↓
IN PROGRESS
   ↓
RESOLVED
```

Emergency resources are not automatically dispatched without human approval.

---

# 🏗️ System Architecture

```text
                     ┌──────────────────────┐
                     │      ResQRoute       │
                     │    Web Dashboard     │
                     └──────────┬───────────┘
                                │
                 ┌──────────────┴──────────────┐
                 │                             │
                 ▼                             ▼
        ┌─────────────────┐          ┌─────────────────┐
        │  Google Maps    │          │ Live Location   │
        │ Maps / Routes   │          │ Browser GPS     │
        └────────┬────────┘          └────────┬────────┘
                 │                            │
                 └──────────────┬─────────────┘
                                ▼
                     ┌──────────────────────┐
                     │   Spring Boot API    │
                     │      Backend         │
                     └──────────┬───────────┘
                                │
             ┌──────────────────┼──────────────────┐
             │                  │                  │
             ▼                  ▼                  ▼
      ┌─────────────┐    ┌─────────────┐   ┌─────────────┐
      │  TinyFish   │    │ PostgreSQL  │   │  Routing &  │
      │ Web Intel.  │    │  Database   │   │  Decision   │
      └─────────────┘    └─────────────┘   └─────────────┘
             │                  │                  │
             └──────────────────┴──────────────────┘
                                │
                                ▼
                     ┌──────────────────────┐
                     │ Emergency Dashboard  │
                     │ Incident + Resources│
                     │ Route + Assignment   │
                     └──────────────────────┘
```

---

# 🧩 Technology Stack

## Frontend

- React
- Vite
- JavaScript
- CSS / existing project styling
- Google Maps JavaScript integration

## Backend

- Java
- Spring Boot
- Maven
- REST APIs

## Database

- PostgreSQL

## External Services

- Google Maps Platform
- TinyFish Web Intelligence API
- Browser Geolocation API

## Development

- Git
- GitHub
- VS Code / Antigravity

---

# 📁 Project Structure

```text
ResQRoute/
│
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   │   ├── CitySelector.jsx
│   │   │   ├── GoogleResQMap.jsx
│   │   │   ├── FallbackResQMap.jsx
│   │   │   ├── LiveLocationControl.jsx
│   │   │   ├── ReportCard.jsx
│   │   │   ├── ResQMap.jsx
│   │   │   └── RouteLegend.jsx
│   │   │
│   │   ├── hooks/
│   │   │   ├── useGoogleMaps.js
│   │   │   ├── useLiveLocation.js
│   │   │   └── useResQRoute.js
│   │   │
│   │   ├── pages/
│   │   │   └── Dashboard.jsx
│   │   │
│   │   ├── services/
│   │   │   └── api.js
│   │   │
│   │   └── data/
│   │
│   ├── package.json
│   └── .env.example
│
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/resqroute/
│   │   │   │       ├── controller/
│   │   │   │       ├── dto/
│   │   │   │       ├── model/
│   │   │   │       └── service/
│   │   │   │
│   │   │   └── resources/
│   │   │       └── application.yml
│   │   │
│   │   └── test/
│   │
│   └── pom.xml
│
├── scripts/
├── docker-compose.yml
├── .env.example
└── README.md
```

> The exact structure may evolve as the backend and frontend are extended.

---

# ⚙️ Installation & Setup

## Prerequisites

Make sure the following are installed:

- Node.js
- npm
- Java JDK
- PostgreSQL
- Git

For live integrations, you also need:

- Google Maps API key
- TinyFish API key

---

# 🔑 Environment Variables

## Frontend

Create:

```text
frontend/.env
```

Example:

```env
VITE_GOOGLE_MAPS_API_KEY=YOUR_GOOGLE_MAPS_API_KEY
```

Never commit the actual API key.

---

## Backend

Configure the required environment variables according to the existing backend configuration.

Example:

```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=resqroute
DB_USER=resqroute
DB_PASSWORD=your_database_password

TINYFISH_API_KEY=your_tinyfish_api_key
TINYFISH_MOCK_MODE=false

SERVER_PORT=8080
```

Use mock mode when testing without a live TinyFish API:

```env
TINYFISH_MOCK_MODE=true
```

---

# ▶️ Running the Frontend

Open a terminal:

```bash
cd frontend
```

Install dependencies:

```bash
npm install
```

Start the development server:

```bash
npm run dev
```

The frontend will normally be available at the Vite development URL shown in the terminal.

---

# ▶️ Running the Backend

Open another terminal:

```bash
cd backend
```

If the Maven wrapper is correctly configured:

```powershell
.\mvnw.cmd spring-boot:run
```

Alternatively, if Maven is installed:

```bash
mvn spring-boot:run
```

The backend is configured to run on:

```text
http://localhost:8080
```

with the configured API context path.

---

# 🗄️ Database

ResQRoute uses PostgreSQL for persistent application data.

Create the required database before starting the backend.

Example:

```sql
CREATE DATABASE resqroute;
```

Then configure:

```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=resqroute
DB_USER=resqroute
DB_PASSWORD=your_database_password
```

The project's Spring Boot/JPA configuration manages the application's database schema according to the current configuration.

---

# 🔐 API Security

The following principles are followed:

- API keys are stored in environment variables.
- TinyFish credentials remain on the backend.
- Google Maps browser credentials should use appropriate API restrictions.
- Secrets must not be committed to GitHub.
- Live location requires explicit user permission.
- Dispatcher actions should be protected by authorization when authentication is available.

---

# 🔄 Complete Workflow

```text
1. Select City
        ↓
2. Open City Map
        ↓
3. Rescue Team Shares Location
        ↓
4. TinyFish Collects Disaster Reports
        ↓
5. Reports Are Parsed & Normalized
        ↓
6. Incident Location Is Matched
        ↓
7. Incident Confidence & Priority Calculated
        ↓
8. Incident Appears on Map
        ↓
9. Resources Recommended
        ↓
10. Dispatcher Reviews Incident
        ↓
11. Route Planned
        ↓
12. Rescue Team Assigned
        ↓
13. Incident Status Updated
        ↓
14. Incident Resolved
```

---

# 🧪 Testing Strategy

The project should test the following areas:

### Frontend

- City selection
- Map loading
- Missing API key handling
- Location permission handling
- Start/stop tracking
- Incident rendering
- Loading states
- Empty states
- API failure states

### Backend

- TinyFish response parsing
- Mock mode
- API failure handling
- Disaster classification
- Location matching
- Duplicate detection
- Confidence calculation
- Incident prioritization
- Resource recommendation
- Route API failures
- Assignment workflow

External services should be mocked during automated testing wherever possible.

---

# ⚠️ Important Limitations

ResQRoute is designed as an intelligent decision-support system. It does not replace official emergency services or human dispatchers.

### Disaster Detection

TinyFish can collect information from online sources, but online information may be:

- Incomplete
- Delayed
- Incorrect
- Conflicting

Therefore, collected information should be treated according to its confidence and verification status.

### Google Maps

Google Maps provides mapping and routing capabilities but does not guarantee awareness of every disaster or road blockage.

### Live Location

Live tracking requires:

- User permission
- A supported device/browser
- Location services

### Resource Availability

The system should not claim that a fire brigade, ambulance, rescue boat, or other resource is physically available unless real resource-inventory data is connected.

### Emergency Dispatch

ResQRoute does not automatically dispatch real emergency services.

A human dispatcher should review and approve operational decisions.

---

# 🚀 Future Scope

Possible future improvements include:

- Real-time government disaster feeds
- Official weather and flood APIs
- Real rescue-team fleet tracking
- IoT-based disaster sensors
- Satellite imagery integration
- Computer vision for damage detection
- Advanced road-risk prediction
- Offline emergency operation
- Multi-city and nationwide deployment
- Real emergency-service integration
- Historical disaster analytics
- Predictive disaster-risk mapping
- Mobile application for rescue teams
- Automated alert notifications
- Multi-agency coordination

---

# 🎯 Project Impact

ResQRoute aims to reduce the time between **disaster detection and emergency response** by bringing information, location, routing, and resource planning into a single platform.

Instead of requiring rescue teams to manually search multiple sources, ResQRoute provides a centralized interface for:

```text
Discover → Verify → Prioritize → Plan → Respond
```

This can help emergency coordinators make faster and more informed decisions while keeping final operational control with authorized personnel.

---

# 🏆 Project Highlights

- 🌍 City-wise disaster visualization
- 📍 Live rescue-team location
- 🗺️ Google Maps integration
- 🔎 Web-based disaster intelligence
- 🤖 TinyFish integration
- 🚨 Multi-disaster incident detection
- 📊 Incident confidence and prioritization
- 🚒 Disaster-specific resource recommendations
- 🛣️ Route planning
- 👨‍🚒 Dispatcher-controlled rescue assignment
- 🔐 Environment-based API configuration
- ⚡ Full-stack emergency-response workflow

---

# 👥 Team

**Project:** ResQRoute

**Domain:** Disaster Management / Emergency Response / Intelligent Routing

**Repository:**  
https://github.com/SHREYA-03112006/ResQRoute

---

# 📌 Development Phases

### Phase 1
**City-Wise Map & Live Location**

Google Maps + city selection + live rescue-team location.

### Phase 2
**TinyFish Disaster Intelligence**

Web-based disaster information collection + incident processing + map integration.

### Phase 3
**Rescue Planning & Response**

Incident prioritization + resource recommendation + route planning + rescue assignment + final dashboard.

---

# 📄 License

Add the project's intended license here if the team decides to publish the project under an open-source license.

---

## 🚨 ResQRoute

> **Detect Faster. Respond Smarter. Save Lives.**
