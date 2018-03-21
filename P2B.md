# GroupMessenger2    

### Structure details for for this project    

- There are 3 main thread in this project
    > We may most focus on QueueTask which implements the algorithms
    
    1. MainActivity / ServerTask (Receive messages and update UI)
    2. ClientTask (Send messages to target endpoint)
    3. QueueTask (Handling the message)

- QueueTask Structure
    > Principle: First commucation ; Second handle other stuff;

    ```
    EditText:
        Button Event:
            Put content to SendQueue.
    ```
    
    ```
    ServerTask:
        Socket accept:
            Put message to RecvQueue.
    ```
    
    ```
    Main Loop:
        if RecvQueue not empty:
            Put messages to HandleQueue.
        
        if SendQueue not empty:
            Send messages
        
        if both RecvQueue and SendQueue are empty:
            Handle messages
    ```
    
    - EditText(Button) -> SendQueue -> QueueTask -> ClientTask
    - ServerTask -> RecvQueue -> QueueTask
    - QueueTask -> SendQueue -> ClientTask

- Algorithm
    - Failure-dector: Heartbeat
    - FIFO: Clock vector
    - Total Ordering: ISIS
    
### Detail of algorithm    
> A device means a process here.

- Failure-dector: Heartbeat
    > Send with a heartbeat signal, return a alive signal    
    > Determine endpoint status with a counter      
    > For this project, the crush will not cause disconnection which means you can still send msg but get no reply     
    > The counter imply the msg that endpoint didnt reply    
    
    1. Send original msg with a heartbeat signal
        > Assume a signal is also a msg with a specific flag.
        
        1. Send original msg
        2. Send heartbeat signal
        3. Increase the heartbeat counter by 1
            - To group: [0, 0, 0, 0, 0] -> [1, 1, 1, 1, 1]
            - To specific endpoint: [0, 0, 0, 0, 0] -> [0, 0, 1, 0, 0]
        4. Update endpoint status
            - [true/false, ...]
            
    2. Deliver a normal message with heartbeat signal
        1. Transfer original message to handle queue
        2. Add a alive signal to send Queue
        3. Send the alive signal to the specific endpoint
        
    3. Deliver a single alive message
        1. Decrease the heartbeat counter by 1
            - [1, 1, 1, 1, 1] -> [0, 0, 0, 0, 1] 
        2. Update endpoint status
- FIFO
    - Use a logical clocks to determine the order
    - Basically increments its counter when executing a send or receive event except heartbeat and alive signal
    
    - Sequence Vector

### Main working flow
0. Button Event    
    - Build INIT-MSG from EditText    
    - Put INIT-MSG to SendQueue    

1. INIT SEND     
    - Get INIT-MSG from SendQueue    
    - Send INIT-MSG with Heartbeat singal to GROUP        
    
2. INIT RECV     
    - Return alive signal to DEVICE        
    - Parse MSG and store its info        
        - <MID, SenderPID, Content>    
    - Put this MSG into PriorQueue    
    - Calculate proposed priority    
    - Build REPLY-MSG        
    - Put REPLY-MSG to SendQueue        

3. REPLY SEND        
    - Get REPLY-MSG from SendQueue    
    - Send REPLY-MSG with Heartbeat signal to DEVICE    

4. REPLY RECV    
    - Return alive signal to DEVICE        
    - Parse MSG and get info        
        - <SenderPID, MID, PropPrior>    
    - Wait until get all device return    
        - Detect device connection with heartbeat signal    
    - Calculate agreed priority for this MSG    
    - Set deliverable for this MSG
    - Build DELIVER-MSG    
    - Put DELIVER-MSG to SendQueue    

5. DELIVER SEND    
    - Get DELIVER-MSG from SendQueue    
    - Send DELIVER-MSG with heartbeat signal to GROUP    

6. DELIVER RECV (from device)      
    - Return alive signal to DEVICE    
    - Parse MSG and get info    
        - <AgreedPrior, Deliverable>    
    - Set deliverable for this MSG    
    - Order the PriorQueue according to this info    

### Data Structure
- Message 
    - mid: `1-{pid}-{counter}` (NO CHANGE)
        - Show msg created by which device (pid) and the order of the msg created by this device (counter)
    - msgContent (NO CHANGE)
    - msgType
    - sPID
        - Which device send this msg this time.

