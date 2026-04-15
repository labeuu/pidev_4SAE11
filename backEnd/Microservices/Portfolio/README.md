# 📂 Portfolio Microservice

**Canonical platform doc:** [Documentation/services/portfolio.md](../../Documentation/services/portfolio.md)

The **Portfolio Microservice** is a core component of the **Freelancia** platform, designed to manage freelancer profiles, skills, and professional experiences. It also features an AI-powered assessment engine to verify skills through automated technical tests.

## 🚀 Features

### 1. **Skill Management**
   - **User-Specific Skills**: Freelancers can add and manage their technical skills.
   - **Verification System**: Skills are initially "Unverified" and can be verified via AI assessments.
   - **Domain Grouping**: Skills are categorized by domain (e.g., Frontend, Backend, DevOps).

### 2. **Experience Tracking**
   - **Career Timeline**: Log professional experiences (Jobs, Projects, Internships).
   - **Smart Linking**: Adding an experience automatically suggests or links relevant skills to the user's profile.
   - **Rich Details**: Track roles, companies, dates, descriptions, and key tasks.

### 3. **AI Assessment Engine**
   - **Automated Test Generation**: Generates unique multiple-choice questions (MCQs) for a specific skill using an integrated AI service.
   - **Instant Grading**: Evaluates answers in real-time and calculates a score.
   - **Certification**: Automatically verifies a skill if the user passes the threshold score.

### 4. **Portfolio Overview**
   - **Unified Profile**: Aggregates skills, experiences, and test results into a comprehensive freelancer profile.

---

## 🛠️ Tech Stack

- **Framework**: Spring Boot 3.x
- **Language**: Java 17+
- **Database**: MySQL (`portfolio_db`)
- **Discovery**: Netflix Eureka Client
- **Architecture**: RESTful API, Microservices
- **AI Integration**: External LLM API (via RestTemplate)

---

## 🔌 API Endpoints

### **Skills**
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/skills/user/{userId}` | Get all skills for a specific user |
| `POST` | `/api/skills` | Add a new skill |
| `DELETE` | `/api/skills/{id}` | Remove a skill |

### **Experiences**
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/experiences/user/{userId}` | Get career history for a user |
| `POST` | `/api/experiences` | Add a new work experience |
| `PUT` | `/api/experiences/{id}` | Update an existing experience |

### **Evaluations (AI Tests)**
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/evaluation-tests/generate/{skillId}` | Generate an AI test for a skill |
| `POST` | `/api/evaluations/submit` | Submit test answers for grading |
| `GET` | `/api/evaluations/freelancer/{id}` | Get evaluation history |

---

## ⚙️ Configuration

### **Application Properties**
The service runs on port **8086** by default.

```properties
server.port=8086
spring.application.name=PORTFOLIO
spring.datasource.url=jdbc:mysql://localhost:3307/portfolio_db
```

### **Running the Service**

1.  **Prerequisites**: Ensure **MySQL** is running and the **Eureka Server** (port 8420) is up.
2.  **Build**:
    ```bash
    mvn clean install
    ```
3.  **Run**:
    ```bash
    mvn spring-boot:run
    ```

---

## 🏗️ Architecture Notes

- **Data Model**: The service moved from a global skill repository to a user-centric model where `Skill` entities are owned by specific users (`userId`).
- **Integration**: Works in tandem with the **User Microservice** (for identity) and **API Gateway** (for routing).
