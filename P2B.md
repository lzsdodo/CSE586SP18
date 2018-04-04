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
    - Total Ordering: ISIS
    
### Detail of algorithm
> A device means a process here.

- Failure-dector: Heartbeat
    > Send with a heartbeat signal, return a alive signal    
    > Determine endpoint status with a counter      
    > For this project, the crush will not cause disconnection which means you can still send msg but get no reply     
    > The counter imply the msg that endpoint didnt reply    
    
    1. Send heartbeat signal per second
        > Assume a signal is also a msg with a specific flag.
        
        1. Send heartbeat signal
        2. Increase the heartbeat counter by 1
            - To group: [0, 0, 0, 0, 0] -> [1, 1, 1, 1, 1]
            - To specific endpoint: [0, 0, 0, 0, 0] -> [0, 0, 1, 0, 0]
        3. Update endpoint status
            - [true/false, ...]
            
    2. Deliver a single alive message
        1. Decrease the heartbeat counter by 1
            - [1, 1, 1, 1, 1] -> [0, 0, 0, 0, 1] 
        2. Update endpoint status
    
- TO algorithm: ISIS
    - Basically increments its counter when executing a send or receive event except heartbeat and alive signal
    - After collect all the reply message, calculate the highest proirity and deliver it.
    - Re B-multicast the original message with its final priority for this message.

### Main working flow
0. Button Event    
    - Build INIT-MSG from EditText    
    - Put INIT-MSG to SendQueue    

1. SEND INIT-MSG      
    - Get INIT-MSG from SendQueue 
    - Send to device
    
2. RECV INIT                
    - Parse MSG and store its info        
        - <MID, SenderPID, Content, ...>    
    - Put this MSG into PriorQueue    
    - Calculate proposed priority    
    - Build REPLY-MSG        
    - Put REPLY-MSG to SendQueue        

3. SEND REPLY             
    - Get REPLY-MSG from SendQueue 
    - Send to device      

4. RECV REPLY          
    - Parse MSG and collect info        
        - <MID, PropPID, PropPrior>    
    - Wait until get all device return    
        - DETECT CONNECTION WHILE WAITING FOR ALL REPLY MSG        
    - Calculate agreed priority for this MSG    
    - Build DELIVER-MSG    
    - Put DELIVER-MSG to SendQueue    

5. SEND DELIVER     
    - Get DELIVER-MSG from SendQueue    
    - Send DELIVER-MSG to GROUP    

6. RECV DELIVER         
    - Parse MSG and get info    
        - <MID, PropPID, AgreedPrior, Deliverable>    
    - Set deliverable for this MSG    
    - Order the PriorQueue according to this info    

