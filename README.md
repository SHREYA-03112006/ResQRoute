# ResQ-Route — AI-Powered Disaster Logistics & Safe-Route Planning

> **Human-in-the-Loop Emergency Route Planning System**  
> Identifies road closures, flooded roads, and damaged bridges from public disaster advisories, matches them to OpenStreetMap road networks, and calculates resilient alternate routes for disaster-relief dispatchers.

---

## 🏗 Mandatory Technology Stack

| Layer | Technology |
|---|---|
| **Backend** | Java 17 + Spring Boot 3.2.5 |
| **Database** | PostgreSQL 16 + PostGIS 3.4 (with in-memory H2GIS standalone profile) |
| **Caching** | Redis 7.2 (with fallback Spring Cache in standalone mode) |
| **Map Engine** | OpenStreetMap + React Leaflet |
| **Frontend** | React 19 + Vite 8.3 |
| **Web Intelligence** | TinyFish API (Search, Fetch, Agent) *(Phase 3)* |
| **Routing Algorithm** | Dijkstra & A* *(Phase 2)* |
| **Communication** | REST APIs with unified error handling and CORS support |

---

## 📁 Project Architecture & Directory Structure

```
ResQRoute/
├── .env.example                      # Environment variable template
├── docker-compose.yml                # PostgreSQL + PostGIS & Redis container definitions
├── scripts/
│   └── init-db.sql                   # PostGIS & pg_trgm extension initialization
├── backend/
│   ├── pom.xml                       # Maven build descriptor with Spring Boot & Hibernate Spatial
│   ├── mvnw.cmd                      # Maven Wrapper for Windows (pointing to Apache Maven 3.9.9)
│   └── src/
│       ├── main/
│       │   ├── java/com/resqroute/
│       │   │   ├── ResQRouteApplication.java    # Application entry point
│       │   │   ├── config/                      # CORS and Redis Cache configurations
│       │   │   ├── controller/                  # REST Controllers (/locations, /reports, /status)
│       │   │   ├── model/                       # Entities: Location, RoadReport, RoadStatus, RouteRequest
│       │   │   ├── repository/                  # Spring Data JPA Repositories
│       │   │   └── service/                     # Business logic & DataSeeder
│       │   └── resources/
│       │       ├── application.yml              # PostgreSQL + Redis production configuration
│       │       └── application-local.yml        # Zero-dependency local developer profile
│       └── test/                                # JUnit 5 smoke & context integration tests
└── frontend/
    ├── package.json                  # React 19, Leaflet, Axios, Vite dependencies
    ├── vite.config.js                # Vite bundler configuration
    ├── index.html                    # HTML shell with accessibility and SEO metadata
    └── src/
        ├── components/               # Header, ResQMap, ReportCard, RoutePanel, RouteLegend
        ├── pages/Dashboard.jsx       # Dispatcher workspace with 3-column layout
        ├── services/api.js           # Central Axios HTTP client
        └── index.css                 # Responsive design system & dark mode aesthetics
```

---

## ⚙ Prerequisites & Requirements

1. **Java JDK 17** (e.g. Eclipse Adoptium OpenJDK 17)
2. **Node.js** (v18+ or v20+) and **npm**
3. **Database Options**:
   - **Container Mode (Production)**: Docker Desktop running `docker-compose up -d` (PostgreSQL 16 on `5432` with PostGIS, Redis on `6379`).
   - **Standalone Mode (Local Dev)**: Runs out of the box using the `local` Spring Boot profile (`application-local.yml`), which uses in-memory H2 with spatial dialect and Spring cache without requiring external daemons.

---

## 🚀 Step-by-Step Instructions to Run Phase 1

### 1. Configuration

Copy `.env.example` to `.env` if custom overrides are needed:
```bash
cp .env.example .env
```

Note: On Windows systems running Oracle Database (TNSLSNR on port 8080), ResQ-Route defaults to port `8081` to avoid port collisions:
- Backend: `http://localhost:8081/api`
- Frontend: `http://localhost:5173`

### 2. Run the Backend

Navigate to `ResQRoute/backend`:
```powershell
cd ResQRoute/backend

# Option A: Standalone Local Development (Recommended for quick start)
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"

# Option B: With Docker (PostgreSQL 16 + PostGIS + Redis)
# First start containers: docker-compose up -d
.\mvnw.cmd spring-boot:run
```

Verify backend health:
```powershell
curl.exe http://localhost:8081/api/status
```

### 3. Run the Frontend

In a separate terminal, navigate to `ResQRoute/frontend`:
```powershell
cd ResQRoute/frontend
npm install
npm run dev
```

Open your browser at: **`http://localhost:5173`**

---

## 📡 Phase 1 Verified REST API Endpoints

| Method | Endpoint | Description | Sample Output |
|---|---|---|---|
| `GET` | `/api/status` | System health, pilot district, data mode, and server timestamp | `{"dataMode":"SAMPLE","pilotDistrict":"Chamoli / Rudraprayag, Uttarakhand",...}` |
| `GET` | `/api/locations` | All relief depots, camps, and hospitals | 9 seeded locations across Rudraprayag & Chamoli |
| `GET` | `/api/reports` | Public road-condition hazard reports | 3 sample reports (`BLOCKED`, `FLOODED`, `UNCERTAIN`) |
| `POST` | `/api/locations` | Register a new depot or relief camp | Creates Location entity |
| `PATCH` | `/api/reports/{id}/review` | Dispatcher updates report review notes | Flags or clears reports |
