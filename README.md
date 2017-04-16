# SpamIt
Spamming Scenario application using JADE

Command line argument to run all the agents 

-----GUI boot---
-gui -host localhost -port 420

-----SA-----
-container -host localhost -port 420 -agents "SpammerAgent:SpammerAgent(50,1)"

-----MCA-----
-container -host localhost -port 420 -agents "MessageConsumingAgent:MessageConsumingAgent(50)"

------EMA------
-container -host localhost -port 420 -agents "ExperimentMasterAgent:ExperimentMasterAgent()"

---WholeScenarioSingleContainer-----
-gui -host localhost -port 420 -agents "MessageConsumingAgent:MessageConsumingAgent(50);SpammerAgent:SpammerAgent(50,1);ExperimentMasterAgent:ExperimentMasterAgent()"
