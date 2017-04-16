import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;

import java.util.Random;

/**
 * Spammer Agent (SA). Sends N messages of size M to all MessageConsumingServices's when it receives
 * a START message from ExperimentMasterAgent.
 * Run:
 * java jade.Boot -container SpammerAgent:SpammerAgent(N, M)
 * - N: number of messages.
 * - M: size of each message.
 */
public class SpammerAgent extends Agent {

    private final Logger logger = Logger.getMyLogger(getClass().getName());
    private static final long serialVersionUID = -3669628420932251804L;
    final static String LANGUAGE = "spam";

    private int numberOfMessages ;
    private int sizeOfEachMessage;

    @Override
    protected void setup() {
        super.setup();
        Object[] args = getArguments();

        if (args != null && args.length == 2) {
            numberOfMessages = Integer.parseInt((String) args[0]);
            sizeOfEachMessage = Integer.parseInt((String) args[1]);
            logger.log(Logger.INFO, "Agent " + getLocalName() + " - Target: " + numberOfMessages + " msg / " + sizeOfEachMessage + " size");

        } else {

            numberOfMessages = 1;
            sizeOfEachMessage = 3;
            logger.log(Logger.INFO, "Agent " + getLocalName() + " - Target: " + numberOfMessages + " msg / " + sizeOfEachMessage + " size");

        }

        ServiceDescription sd = new ServiceDescription();
        sd.setType("SpammerAgent");
        sd.setName("SpammerAgentService");
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            logger.log(Logger.SEVERE, "Agent " + getLocalName() + " - Cannot register with DF", e);
            doDelete();
        }

        addBehaviour(new SimpleBehaviour(this) {

            private static final long serialVersionUID = -1344483830624564835L;
            private boolean start = false;

            @Override
            public void action() {
                MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchSender(new AID("ExpirementMasterAgent", AID.ISLOCALNAME)),
                        MessageTemplate.MatchContent(ExperimentMasterAgent.START));
                ACLMessage msg = myAgent.receive(mt);
                if (msg != null) {
                    // Start spamming MCA's
                    myAgent.addBehaviour(new SpammerBehaviour());
                    start = true;
                } else {
                    block();
                }
            }

            @Override
            public boolean done() {
                return start;
            }
        });


    }

    private class SpammerBehaviour extends OneShotBehaviour{

        private static final long serialVersionUID = -8492387448755961987L;
        /** List of known Message Consuming Agents (MCA) */
        private AID[] MCAs;

        @Override
        public void action() {

            ServiceDescription sd = new ServiceDescription();
            sd.setType("MessageConsumingAgent");
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.addServices(sd);

            try {
                DFAgentDescription[] result = DFService.search(myAgent, dfd);
                logger.log(Logger.INFO, "Found " + result.length + " MCA's");
                MCAs = new AID[result.length];
                for (int i = 0; i < result.length; ++i) {
                    MCAs[i] = result[i].getName();
                }
            } catch (FIPAException e)
            {
                logger.log(Logger.SEVERE, "Cannot get MCA's", e);
            }

            String content = "";
            for (int i = 0; i < sizeOfEachMessage; i++) {
                Random r = new Random();
                content += (char)(r.nextInt(26) + 'a');
            }

            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            for (AID MCA : MCAs) {
                msg.addReceiver(MCA);
            }
            msg.setContent(content);
            msg.setLanguage(LANGUAGE);
            for (int i = 0; i < numberOfMessages; i++) {
                myAgent.send(msg);
            }


        }
    }
}
