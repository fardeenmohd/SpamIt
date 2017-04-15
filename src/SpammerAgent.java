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

/**
 * Spammer Agent (SA). Sends N messages of size M to all MessageConsumingServices's when it receives
 * a START message from ExperimentMasterAgent.
 * Run:
 * java jade.Boot -container SpammerAgent:SpammerAgent(N, M)
 * - N: number of messages.
 * - M: size of each message.
 */
public class SpammerAgent extends Agent {

    public final static String LANGUAGE = "spam";
    private int numberOfMessages ;
    private int sizeOfEachMessage;

    @Override
    protected void setup() {
        super.setup();
        Object[] args = getArguments();

        if (args != null && args.length == 2) {
            numberOfMessages = Integer.parseInt((String) args[0]);
            sizeOfEachMessage = Integer.parseInt((String) args[1]);

        } else {

            numberOfMessages = 1;
            sizeOfEachMessage = 3;

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
            doDelete();
        }

        addBehaviour(new SimpleBehaviour(this) {

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

        private AID[] MCAs;

        @Override
        public void action() {

            ServiceDescription sd = new ServiceDescription();
            sd.setType("MessageConsumingAgent");
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.addServices(sd);

            try {
                DFAgentDescription[] result = DFService.search(myAgent, dfd);

                MCAs = new AID[result.length];
                for (int i = 0; i < result.length; ++i) {
                    MCAs[i] = result[i].getName();
                }
            } catch (Exception e)
            {
            
            }

            String content = "";
            for (int i = 0; i < sizeOfEachMessage; i++) {
                content += "A";
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
