### Case Study Write-Up: Implementation Details of Service A and Service B
### Before Running, Please run - **mvn clean package**.
#### How Service A Manages Transactions
Service A is a Spring Boot REST service running on port 7080, designed to handle user requests by working with Service B. One of the trickiest parts was figuring out how to manage transactions properly, especially since we’re dealing with both a local database and an external service call.

- **What We Did**: At first, in `UserRequestServiceImpl.java` (version #11), I tried wrapping the whole `processRequest` method in a single `@Transactional` block. This covered saving a `TransactionLog` with an `INITIATED` status and calling Service B through `ServiceBClient`. But I quickly realized this wouldn’t work well—Spring can’t undo an HTTP call to Service B if something goes wrong after it’s sent, which could leave us with half-finished work.  
  So, I split it up into smaller, more manageable pieces:
    1. **Step 1 - Logging the Start**: In `createAndSaveInitialLog`, marked with `@Transactional`, we save a `TransactionLog` set to `INITIATED` in our H2 database (configured in `Application.yaml` #13 as `jdbc:h2:mem:testdb`). This gets committed right away, so even if everything else fails, we’ve got a record of the request.
    2. **Step 2 - Calling Service B**: Next, we call Service B’s `/api/serviceB/process` endpoint using `serviceBClient.processTransaction`. This happens outside any transaction, so we don’t lock up the database while waiting for a response.
    3. **Step 3 - Updating the Log**: Depending on what Service B says, we either run `updateLogSuccess` or `updateLogFailed` (both `@Transactional`) to set the `TransactionLog` to `SUCCESS` or `FAILED`. If something goes wrong—like Service B crashing or timing out—we trigger a compensation call to `/api/compensation/{requestId}`.

  Here’s how it looks in `UserRequestServiceImpl.java` (#11):
  ```java
  public ResponseDTO processRequest(RequestDTO requestDTO) {
      TransactionLog transactionLog = createAndSaveInitialLog(requestDTO);
      try {
          ResponseDTO response = serviceBClient.processTransaction(requestDTO);
          updateLogSuccess(transactionLog, response);
          return response;
      } catch (Exception e) {
          updateLogFailed(transactionLog, e);
          triggerCompensation(requestDTO.getRequestId());
          throw new RuntimeException("Service B is unavailable: " + e.getMessage(), e);
      }
  }
  ```
- **Why This Works**: By breaking it into these chunks, we keep database operations quick and avoid holding locks too long. If Service B fails, the `INITIATED` log sticks around, and we can pick it up later with our recovery process.

- **Sketch of the Process**: Imagine a big box labeled "Service A’s Workflow":
    - It starts with "User sends POST /api/user-requests" (from `UserRequestController.java` #10).
    - Then there’s a little box inside called "Tx1: Log INITIATED" (from `createAndSaveInitialLog`).
    - An arrow points to "Call Service B" (no transaction here, just `ServiceBClient.java` #8).
    - From there, it splits:
        - One path goes to "Tx2: Log SUCCESS" and out to "Send 200 OK response."
        - The other goes to "Tx2: Log FAILED," then "Call Compensation," and ends with "Send 500 error."
    - Dashed lines mark the transaction parts (Tx1 and Tx2), and a dotted line loops back to show that `INITIATED` stays if Service B flops.

#### Threading in Service A
Service A runs on Spring Boot with Tomcat, and I set the thread pool to a max of 200 in `Application.yaml` (#13).

- **How It’s Set Up**:
    - When a request hits `/api/user-requests` (`UserRequestController.java` #10), Tomcat grabs a thread from its pool and hands it off to the controller. From there, it’s a straight handoff to `UserRequestServiceImpl.processRequest`, all in that same thread.
    - At one point, I experimented with making the Service B call asynchronous in `UserRequestServiceImpl.java` (#11). I used `@Async` with a custom `TaskExecutor` defined in `AppConfig.java` (#2)—10 core threads, up to 50 max, with a queue of 100. It looked like this:
      ```java
      @Async("taskExecutor")
      public CompletableFuture<ResponseDTO> callServiceBAsync(RequestDTO requestDTO) {
          return CompletableFuture.completedFuture(serviceBClient.processTransaction(requestDTO));
      }
      ```
      But I switched back to a synchronous call:
      ```java
      ResponseDTO response = serviceBClient.processTransaction(requestDTO);
      ```
      Why? It fits better with the split transactions—I didn’t want the database stuff happening in one thread and the Service B call in another. Plus, Feign’s timeouts (5 seconds for connect and read, set in `Application.yaml` #13) keep it from hanging too long.
    - For recovery, though, I kept things async. In `RecoveryService.java` (#New), the periodic checks (every 5 minutes via `@Scheduled`) run on that same `TaskExecutor`, so they don’t clog up the main request threads.
- **What It Means for the Code**:
    - The controller (`UserRequestController.java` #10) just passes the buck to the service layer, all in one Tomcat thread.
    - The service (`UserRequestServiceImpl.java` #11) holds onto that thread while it talks to Service B, which makes sure the transaction updates happen in order. It’s not the fastest setup for tons of requests, but it keeps things consistent.
- **Config Details**: Here’s the `TaskExecutor` setup from `AppConfig.java` (#2):
  ```java
  @Bean(name = "taskExecutor")
  public ThreadPoolTaskExecutor taskExecutor() {
      ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
      executor.setCorePoolSize(10);
      executor.setMaxPoolSize(50);
      executor.setQueueCapacity(100);
      executor.setThreadNamePrefix("ServiceB-Async-");
      return executor;
  }
  ```
- **Pros and Cons**: Going synchronous makes the transaction logic cleaner, but if Service B slows down, we might run out of threads. The async recovery helps balance that out by handling cleanup in the background.

#### Transaction Management in Service B
Service B, on port 7081, is another Spring Boot app that backs up Service A with its own transaction setup.

- **How It Works**:
    - **Processing Requests**: In `ProcessingServiceImpl.java` (#12), the `processRequest` method is wrapped with `@Transactional`. It:
        1. Looks for an existing `TransactionLog` by `requestId` to avoid doing the same thing twice.
        2. Sets up a new log with `INITIATED` if it’s fresh.
        3. Switches it to `SUCCESS` if all goes well, or throws an error if not (which rolls back or sets it to `FAILED`).
      ```java
      @Transactional
      public ResponseDTO processRequest(RequestDTO requestDTO) {
          TransactionLog log = transactionLogRepository.findByRequestId(requestDTO.getRequestId())
              .orElse(new TransactionLog());
          if ("SUCCESS".equals(log.getStatus())) return new ResponseDTO("SUCCESS", requestDTO.getRequestId(), "Already processed");
          log.setRequestId(requestDTO.getRequestId());
          log.setStatus("INITIATED");
          transactionLogRepository.save(log);
          log.setStatus("SUCCESS");
          transactionLogRepository.save(log);
          return new ResponseDTO("SUCCESS", requestDTO.getRequestId(), "Request is successfully completed!");
      }
      ```
    - **Handling Compensation**: In `SagaParticipantService.java` (#15), `compensateTransaction` also uses `@Transactional`:
      ```java
      @Transactional
      public ResponseDTO compensateTransaction(String requestId) {
          TransactionLog log = transactionLogRepository.findByRequestId(requestId)
              .orElseThrow(() -> new ServiceBExceptions("Transaction not found: " + requestId));
          if ("COMPENSATED".equals(log.getStatus())) return new ResponseDTO("ALREADY_COMPENSATED", requestId, "Already compensated");
          log.setStatus("COMPENSATED");
          transactionLogRepository.save(log);
          return new ResponseDTO("COMPENSATED", requestId, "Transaction compensated successfully");
      }
      ```
    - **Database Setup**: It uses an H2 in-memory database (`jdbc:h2:mem:serviceBDB`, `Application.yaml` #16) with `ddl-auto: update`.
- **Why This Approach**: Keeping each operation in its own transaction makes sure Service B’s changes are solid on their own. Checking the `requestId` stops us from processing the same thing twice, which is huge for Service A when it’s retrying or recovering.

#### Threading in Service B
Service B runs on Tomcat too, with a default thread pool of 200 (adjustable in `Application.yaml` #16).

- **How It’s Structured**:
    - **Request Handling**: Calls to `/api/serviceB/process` (`ServiceBController.java` #4) and `/api/compensation/{requestId}` (`CompensationController.java` #2) run synchronously on Tomcat threads.
    - **Recovery**: In `RecoveryService.java` (#14), I set up periodic recovery to run async with a `TaskExecutor` (`AppConfig.java` #1, 5 core threads, max 20):
      ```java
      @Async("taskExecutor")
      @Scheduled(fixedRate = 5 * 60 * 1000)
      public void periodicRecovery() {
          List<TransactionLog> transactions = transactionLogRepository.findByStatus("INITIATED");
          recoverTransactions(transactions);
      }
      ```
- **What It Means**:
    - The controller (`ServiceBController.java` #4) and service (`ProcessingServiceImpl.java` #12) stick to the same thread, keeping transactions tight.
    - Recovery (`RecoveryService.java` #14) runs off to the side, so it doesn’t slow down the main request handling.

#### Handling Failures and Fixing Them

##### When the Network Between Service A and B Goes Down
- **Service B Isn’t Responding**:
    - **Service A’s Side**: If `ServiceBClient.processTransaction` fails with an `IOException`, `ReconciliationService.java` (#9) steps in. I’ve got `@CircuitBreaker(name = "serviceB")` set up to trip after 50% of 10 calls fail (`Application.yaml` #13), and it falls back to:
      ```java
      @CircuitBreaker(name = "serviceB", fallbackMethod = "fallbackServiceB")
      public ResponseDTO processRequest(RequestDTO requestDTO) {
          return serviceBClient.processTransaction(requestDTO);
      }
      private ResponseDTO fallbackServiceB(RequestDTO requestDTO, Throwable t) {
          return new ResponseDTO("FALLBACK", requestDTO.getRequestId(), "Service B unavailable");
      }
      ```
      The `TransactionLog` gets marked `FAILED`, and we try to compensate.
    - **Service B’s Side**: It’s offline, so nothing happens until it’s back up and running its recovery.

- **Timeouts or Dropped Connections**:
    - **Service A**: Feign times out after 5 seconds (`Application.yaml` #13). In `ReconciliationService.java` (#9), `@Retry(name = "serviceB")` gives it 3 shots with delays (1s, 2s, 4s). If that doesn’t work, the circuit breaker kicks in, and `triggerCompensation` tries compensation 3 times too:
      ```java
      @Retry(name = "serviceBCompensation")
      public void compensateRequest(String requestId) {
          serviceBClient.compensateTransaction(requestId);
      }
      ```
      The `TransactionLog` ends up as `FAILED`, and Service B’s log switches to `COMPENSATED` if it’s reachable later.
    - **Service B**: If it finished before the timeout, its log is `SUCCESS`. Compensation doesn’t mess with that because of the `requestId` check.

##### Service A Crashing Mid-Request
- **What Could Go Wrong**:
    - Before the first transaction: Nothing’s saved, so the user just tries again.
    - After saving `INITIATED` but before calling Service B: The log’s there, but Service B hasn’t done anything—safe but unfinished.
    - While calling Service B: Service B might finish (`SUCCESS`), but Service A’s log stays `INITIATED`—a mismatch.
    - After Service B responds but before updating to `SUCCESS`: Same deal—Service B’s done, but Service A’s log isn’t updated.
- **Fixing It**: `RecoveryService.java` (#New) handles this on startup and every 5 minutes:
  ```java
  @PostConstruct
  public void recoverOnStartup() {
      List<TransactionLog> incomplete = transactionLogRepository.findByStatusIn(List.of("INITIATED"));
      recoverTransactions(incomplete);
  }
  private void recoverTransactions(List<TransactionLog> transactions) {
      for (TransactionLog log : transactions) {
          RequestDTO requestDTO = new RequestDTO(log.getRequestId(), "recovered", "unknown");
          try {
              ResponseDTO response = reconciliationService.processRequest(requestDTO);
              updateLogSuccess(log, response);
          } catch (Exception e) {
              updateLogFailed(log, e);
              reconciliationService.compensateRequest(log.getRequestId());
          }
      }
  }
  ```
  It picks up `INITIATED` logs, retries the request, and updates them to `SUCCESS` or `FAILED` with compensation if needed.

##### Service B Crashing Mid-Request
- **What Could Happen**:
    - Before its transaction: Nothing’s saved, so Service A retries.
    - During its transaction: The log’s `INITIATED`, but the job’s not done—still okay.
- **Fixing It**: `RecoveryService.java` (#14) sorts it out:
  ```java
  private void recoverTransactions(List<TransactionLog> transactions) {
      for (TransactionLog transaction : transactions) {
          try {
              ResponseDTO response = processingService.processExistingTransaction(transaction);
              transaction.setStatus("SUCCESS");
          } catch (Exception e) {
              sagaParticipantService.compensateTransaction(transaction.getRequestId());
          }
          transactionLogRepository.save(transaction);
      }
  }
  ```
  It finishes or compensates `INITIATED` logs, keeping everything in sync.

---

### Where Everything Lives
- **Service A**:
    - `UserRequestController.java` (#10): Where requests come in.
    - `UserRequestServiceImpl.java` (#11): Manages the split transactions and Service B calls.
    - `ReconciliationService.java` (#9): Handles retries, circuit breakers, and compensation.
    - `RecoveryService.java` (#New): Takes care of recovery.
    - `Application.yaml` (#13): Sets up Tomcat, H2, Feign, and Resilience4j.
- **Service B**:
    - `ServiceBController.java` (#4): Handles process requests.
    - `CompensationController.java` (#2): Manages compensation.
    - `ProcessingServiceImpl.java` (#12): Does the main processing work.
    - `SagaParticipantService.java` (#15): Handles compensation logic.
    - `RecoveryService.java` (#14): Runs recovery tasks.
    - `Application.yaml` (#16): Configures H2 and AOP.


---

🔹 **Next Steps:**
- Ensure **Service B timeout behavior is correctly handled** via circuit breaker.
- Add **rate limiting** if Service B has call restrictions.
- Implement **integration tests** to verify real Service B behavior.
