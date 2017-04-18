import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;

/**
 * Experiment Master Agent (EMA). Initializes the experiment sending START
 * message to all SA's and measures the total time of processing all messages by
 * all MessageConsumingAgent's.
 * Run:
 * java jade.Boot -container ExperimentMasterAgent:ExperimentMasterAgent()
 * Note: The name of the agent must be 'ExperimentMasterAgent'.
 */
public class ExperimentMasterAgent extends Agent {

    private final Logger logger = Logger.getMyLogger(getClass().getName());
    private static final long serialVersionUID = 570376489866952222L;
    final static String DONE = "done";
    final static String START = "start";

    private AID[] SpammerAgents;

    private int numberOfMessageConsumingAgents;

    private double timeInitial;

    @Override
    protected void setup() {
        addBehaviour(new OneShotBehaviour(this) {
            private static final long serialVersionUID = 1582761767744710850L;

            @Override
            public void action() {
                // Get list of Spammer Agents (SA)
                ServiceDescription sd = new ServiceDescription();
                sd.setType("SpammerAgent");
                DFAgentDescription dfd = new DFAgentDescription();
                dfd.addServices(sd);
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, dfd);
                    logger.log(Logger.INFO, "Found " + result.length + " SpammerAgents");
                    SpammerAgents = new AID[result.length];
                    for (int i = 0; i < result.length; ++i) {
                        SpammerAgents[i] = result[i].getName();
                    }
                } catch (FIPAException e) {
                    logger.log(Logger.SEVERE, "Cannot get SA's", e);

                }
                // Get number of Message Consuming Agents (MCA)
                sd = new ServiceDescription();
                sd.setType("MessageConsumingAgent");
                dfd = new DFAgentDescription();
                dfd.addServices(sd);
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, dfd);
                    numberOfMessageConsumingAgents = result.length;
                } catch (FIPAException e) {
                    logger.log(Logger.SEVERE, "Cannot get MessageConsumingAgents", e);
                }
                // Send START message to all SA's
                ACLMessage startMsg = new ACLMessage(ACLMessage.REQUEST);
                for (AID SpammerAgent : SpammerAgents) {
                    startMsg.addReceiver(SpammerAgent);
                }
                startMsg.setContent(ExperimentMasterAgent.START);
                myAgent.send(startMsg);
                // Start timer
                timeInitial = System.nanoTime();
                // Add the behaviour listen to done messages
                addBehaviour(new ListenDoneMessagesBehaviour());
            }
        });
    }

    /**
     * Listen to done messages and stop timer when all messages have been
     * received.
     */

    private class ListenDoneMessagesBehaviour extends Behaviour {

        private static final long serialVersionUID = 4075092804919487501L;
        /** Number of MCA's that have finished */
        private int done;
        private int numOfMessagesConsumedByAllMCAs = 0;
        private double shortestSpamMsgProcessTime = Double.MAX_VALUE;
        private double longestSpamMsgProcessTime = Double.MIN_VALUE;
        ListenDoneMessagesBehaviour() {
            super();
            done = 0;
        }

        @Override
        public void action() {
            // Receive DONE messages
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage msg = myAgent.receive(mt);
            int numOfMessagesProcessedByOneMCA = 0;
            double shortestMessageProcessTimeForOneMCA = 0.0;
            double longestMessageProcessTimeForOneMCA = 0.0;
            if (msg != null && msg.getContent().contains(ExperimentMasterAgent.DONE)) {
                ++done;
                // Get scenario statistics
                String[] statistics = msg.getContent().split("_");
                numOfMessagesProcessedByOneMCA = Integer.parseInt(statistics[1]);
                shortestMessageProcessTimeForOneMCA = Double.parseDouble(statistics[2]);
                longestMessageProcessTimeForOneMCA = Double.parseDouble(statistics[3]);
                // Now we find the shortest/longest time comparing to other MCAs
                if(shortestMessageProcessTimeForOneMCA < shortestSpamMsgProcessTime){
                    shortestSpamMsgProcessTime = shortestMessageProcessTimeForOneMCA;
                }
                if(longestMessageProcessTimeForOneMCA > longestSpamMsgProcessTime){
                    longestSpamMsgProcessTime = longestMessageProcessTimeForOneMCA;
                }
                numOfMessagesConsumedByAllMCAs += numOfMessagesProcessedByOneMCA;
            } else {
                block();
            }
        }

        @Override
        public boolean done() {
            // When all MCA have sent DONE message, we are done
            return done == numberOfMessageConsumingAgents;
        }

        @Override
        public int onEnd() {
            // Print execution time
            double timeFinished = System.nanoTime() - timeInitial;
            double timeFinishedMilliseconds = timeFinished / 1000000;
            System.out.println("Execution time: " + timeFinishedMilliseconds + "ms");
            System.out.println("Num of Messages: " + numOfMessagesConsumedByAllMCAs + " Average time to process 1 spam msg: " + timeFinishedMilliseconds / numOfMessagesConsumedByAllMCAs + "ms");
            System.out.println("Shortest time to process 1 spam msg: " + shortestSpamMsgProcessTime + "ms");
            System.out.println("Longest time to process 1 spam msg: " + longestSpamMsgProcessTime + "ms");
            return 0;
        }
    }



}
