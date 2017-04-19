import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Message Consuming Agent (MCA). Receives and processes the messages sent by
 * SpammerAgent's. When all messages have been processed, it sends message DONE to the
 * ExperimentMasterAgent. It knows how many messages from each SA should receive.
 * Run:
 * java jade.Boot -container MessageConsumerAgentx:MessageConsumerAgent(N)
 * - N: number of messages to receive from each SpammerAgent.
 * Note: Spammer Agents (SA's) must be running before run MessageConsumerAgent's.
 */
public class MessageConsumingAgent extends Agent {

    private final Logger logger = Logger.getMyLogger(getClass().getName());
    private static final long serialVersionUID = 9085335745014921813L;
    private int numberOfMessages;

    private int numberOfSpammerAgents;

    @Override
    protected void setup() {
        // Get number of messages to receive from each SA
        Object[] args = getArguments();
        if (args != null && args.length == 1) {
            numberOfMessages = Integer.parseInt((String) args[0]);
            logger.log(Logger.INFO, "Agent " + getLocalName() + " - Target: " + numberOfMessages + " msg");
        } else {
            numberOfMessages = 1;
            logger.log(Logger.INFO, "Agent " + getLocalName() + " - Target: " + numberOfMessages + " msg");

        }
        // Register the message consuming service in the yellow pages
        ServiceDescription sd = new ServiceDescription();
        sd.setType("MessageConsumingAgent");
        sd.setName("MessageConsumingAgentService");
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            logger.log(Logger.SEVERE, "Agent " + getLocalName() + " - Cannot register with DF", e);
            doDelete();
        }
        // Get number of Spamer Agents (SA)
        sd = new ServiceDescription();
        sd.setType("SpammerAgent");
        dfd = new DFAgentDescription();
        dfd.addServices(sd);
        try {
            DFAgentDescription[] result = DFService.search(this, dfd);
            numberOfSpammerAgents = result.length;
        } catch (FIPAException e) {
            logger.log(Logger.SEVERE, "Cannot get SpammerAgents", e);
        }
        // Add the behaviour consuming spam messages
        addBehaviour(new MessageConsumingBehaviour());
    }

    /**
     * Receive and process each message sent by Spammer Agents. Send DONE
     * message to EMA when all messages have been received.
     */
    private class MessageConsumingBehaviour extends Behaviour {

        private static final long serialVersionUID = -5860119910249641199L;
        /** SA -> nº of msg received by it */
        private static final String SEPERATOR = "_";
        private Map<String, Integer> received; //
        private int numOfMessagesProcessed = 0;
        private double shortestMsgProcessTime = Double.MAX_VALUE;
        private double longestMsgProcessTime = Double.MIN_VALUE;

        MessageConsumingBehaviour() {
            super();
            this.received = new HashMap<>(numberOfSpammerAgents);
        }
        public void updateStatistics(double initialProcessTime){
            double timeToProcessMsg = (double)System.nanoTime() - initialProcessTime;
            if(timeToProcessMsg < shortestMsgProcessTime){
                shortestMsgProcessTime = timeToProcessMsg;
            }
            if(timeToProcessMsg > longestMsgProcessTime){
                longestMsgProcessTime = timeToProcessMsg;
            }
            numOfMessagesProcessed++;
        }
        @Override
        public void action() {
            // Receive spam messages
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchLanguage(SpammerAgent.LANGUAGE));

            double timeToProcessMessageInitial = System.nanoTime();
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                logger.log(Logger.INFO, "Agent " + getLocalName() + " - Message processed: " + msg.getContent());
                // Update register of received messages
                String sender = msg.getSender().getName();
                if (received.containsKey(sender)) {
                    received.put(sender, received.get(sender) + 1);


                } else {
                    received.put(sender, 1);
                }
                updateStatistics(timeToProcessMessageInitial);
                timeToProcessMessageInitial = System.nanoTime();
            } else {
                block();
            }
        }

        @Override
        public boolean done() {
            // Check all expected agents are registered
            if (received.size() != numberOfSpammerAgents) {
                return false;
            }
            // Check if all messages have been received
            for (int i : received.values()) {
                if (i != numberOfMessages) {
                    return false;
                }
            }
            // Send DONE message to EMA
            ACLMessage doneMsg = new ACLMessage(ACLMessage.INFORM);
            doneMsg.addReceiver(new AID("ExperimentMasterAgent", AID.ISLOCALNAME));
            // Convert nanoseconds to milliseconds by dividing by a million
            shortestMsgProcessTime /= 1000000;
            longestMsgProcessTime /= 1000000;
            // Encode all statistics of message processing in done message
            doneMsg.setContent(ExperimentMasterAgent.DONE + SEPERATOR + numOfMessagesProcessed + SEPERATOR + shortestMsgProcessTime + SEPERATOR + longestMsgProcessTime);
            logger.log(logger.INFO, "Sending statistics, longestTime: " + longestMsgProcessTime + " shortestTime: " + shortestMsgProcessTime + " NumOfMessagesProcessed: " + numOfMessagesProcessed);
            myAgent.send(doneMsg);
            return true;
        }
    }



}
