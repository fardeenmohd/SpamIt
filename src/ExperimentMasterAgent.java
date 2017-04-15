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

    public final static String DONE = "done";
    public final static String START = "start";

    private AID[] SpammerAgents;

    private int numberOfMessageConsumingAgents;

    private long timeInitial;

    @Override
    protected void setup() {
        addBehaviour(new OneShotBehaviour(this) {

            @Override
            public void action() {
                // Get list of Spammer Agents (SA)
                ServiceDescription sd = new ServiceDescription();
                sd.setType("SpammerAgent");
                DFAgentDescription dfd = new DFAgentDescription();
                dfd.addServices(sd);
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, dfd);
                    SpammerAgents = new AID[result.length];
                    for (int i = 0; i < result.length; ++i) {
                        SpammerAgents[i] = result[i].getName();
                    }
                } catch (FIPAException e) {

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

                }
                // Send START message to all SA's
                ACLMessage startMsg = new ACLMessage(ACLMessage.REQUEST);
                for (int i = 0; i < SpammerAgents.length; ++i) {
                    startMsg.addReceiver(SpammerAgents[i]);
                }
                startMsg.setContent(ExperimentMasterAgent.START);
                myAgent.send(startMsg);
                // Start timer
                timeInitial = System.currentTimeMillis();
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


        private int done;

        public ListenDoneMessagesBehaviour() {
            super();
            done = 0;
        }

        @Override
        public void action() {
            // Receive DONE messages
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchContent(ExperimentMasterAgent.DONE));
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                ++done;
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
            long timeFinished = System.currentTimeMillis() - timeInitial;
            System.out.println("Execution time: " + timeFinished + "ms");
            return 0;
        }
    }



}
