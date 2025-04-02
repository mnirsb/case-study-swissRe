# **Case Study: Service A & Service B**

## **mvn clean package**

## **Overview**
This case study explains how **Service A** and **Service B** interact with each other, focusing on transactional boundaries, threading models, failure handling, and recovery. It also includes details about the **Docker Compose** setup used for running both services together.

---

## **Service A**
### **Purpose**
Acts as the **entry point** for user requests. It manages the overall workflow by coordinating with **Service B**.

### **Key Features**
- **Transactional Boundaries**: Uses an **H2 in-memory database** to track request states (`INITIATED`, `SUCCESS`, `FAILED`). It follows a **choreography-based Saga pattern**, meaning it triggers compensation if something goes wrong in Service B.
- **Threading Model**:
  - Runs on **Tomcat** (supports up to **200 threads**) to handle incoming requests.
  - Uses an **async thread pool (10-50 threads, queue size 100)** for calling Service B without blocking other requests.
  - If the thread pool is full, it applies a **CallerRunsPolicy**, ensuring no request is dropped.
- **Failure Handling**:
  - Implements **Resilience4j** for **retries and circuit breakers** when calling Service B.
  - If Service B fails, it logs the failure and retries.
  - If the issue persists, it queues compensation and periodically retries (every 5 minutes).
- **Recovery**:
  - On startup, it checks incomplete transactions (`INITIATED`) and attempts recovery.
  - Calls **Service B’s status endpoint** (`/status/{requestId}`) to avoid re-processing completed requests.
- **Docker Setup**:
  - Runs on **port 7080**.
  - Uses environment variables to configure **H2 database** and **Service B’s URL (`http://service-b:7081`)**.

---

## **Service B**
### **Purpose**
Processes requests sent by **Service A** and supports **compensation** if needed.

### **Key Features**
- **Transactional Boundaries**: Uses an **H2 database** to maintain its own **TransactionLog** (`INITIATED`, `SUCCESS`, `FAILED`, `COMPENSATED`).
- **Threading Model**:
  - Runs on **Tomcat** (**200 max threads**) for request handling.
  - Uses an **async thread pool (5-20 threads, queue size 50)** for recovery tasks.
  - Applies **CallerRunsPolicy** to ensure requests are not dropped.
- **Failure Handling**:
  - Uses **Spring Retry (`@Retryable`)** for transient failures (e.g., database issues).
  - If compensation is needed, it notifies **Service A** (`/notify-compensation`).
- **Recovery**:
  - Checks for **incomplete transactions (`INITIATED`)** every **6 minutes** and recovers them.
- **Docker Setup**:
  - Runs on **port 7081**.
  - Provides API endpoints:
    - `/api/serviceB/process`: Processes requests from Service A.
    - `/api/compensation/{requestId}`: Handles compensation for failed transactions.

---

## **How Service A & B Work Together**
Think of this system like ordering food online:

1. **User places an order** (`req123`):
   - Service A logs **INITIATED** and sends a request to Service B.
   - It does this asynchronously (doesn’t wait for a response).

2. **Service B processes the order**:
   - Logs **INITIATED**, completes the work, and updates the log to **SUCCESS**.
   - Responds to Service A with confirmation.

3. **Service A marks the order as complete**:
   - Logs **SUCCESS** and notifies the user that the order is ready.

---

## **Handling Failures**

### **1. What if Service B is down?**
- Service A retries multiple times.
- If still down, it logs **FAILED** and queues compensation.
- Once Service B is back, Service A retries processing or cancels the request.

### **2. What if Service A crashes?**
- When restarted, Service A checks its logs.
- If an order is stuck at **INITIATED**, it asks Service B:
  - If Service B says **SUCCESS**, Service A updates its log.
  - If Service B also failed, Service A retries or compensates.

### **3. What if Service B fails mid-process?**
- Service B logs **FAILED** and triggers compensation.
- It notifies Service A (if notification is enabled).
- Service A then updates its log accordingly.

---

## **Docker Compose Setup**
- Both services run in a **single network (`app-network`)**.
- Service A depends on Service B (`depends_on: service-b`).
- Docker builds JARs using **Maven (`mvn package`)** and copies them into containers.
- Uses **wildcards (`target/*.jar`)** to handle different build names dynamically.

---

## **Summary Flow Diagram**
```
User --> Service A (port 7080)
  |--> Log: INITIATED
  |--> Async Call --> Service B (port 7081)
        |--> Log: INITIATED
        |--> Process --> Log: SUCCESS
  |<-- Response: SUCCESS
  |--> Log: SUCCESS
  |--> User: "Order done!"
```

#### **Failure Example:**
```
User --> Service A
  |--> Log: INITIATED
  |--> Call Service B (down)
  |--> Retry/Fallback --> Log: FAILED
  |--> Queue Compensation
  |--> Recovery --> Retry or Compensate
```

This ensures that even if **one service fails, the system remains reliable and recovers gracefully**.

---

### **Final Thoughts**
This setup ensures:
✅ Orders (requests) are processed smoothly.
✅ No requests are lost, even in failures.
✅ Services run independently but coordinate efficiently.
✅ Docker Compose makes deployment easy.

You can start everything with **one command**, and the system will take care of failures, retries, and recovery automatically!

🚀 **Happy coding!**

