package behaviours;

import agents.AgentCommercial;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * @author Hong-Phuc VU
 * @author Yasmine GUEDJOU
 */

public class TransactionBehaviours extends TickerBehaviour {

    private static final long serialVersionUID = 1L;
    AgentCommercial myAgentCommercial;
    private int runningState;
    private AID[] productorAgents;
    private java.util.logging.Logger logger;

    int quanityIdeal = 0;
    double priceIdeal = 9999;
    private String messageId;

    public TransactionBehaviours(Agent a, long period) {
        super(a, period);
        this.myAgentCommercial = (AgentCommercial) myAgent;
        this.logger = Logger.getMyLogger(this.getClass().getName());
    }

    @Override
    public void onStart() {
        super.onStart();
        this.runningState = 0;
    }

    public int randomGenerator(int min, int max) {
        int range = max - min + 1;
        return (int) (Math.random() * range) + min;
    }

    @Override
    protected void onTick() {
        //Id transaction
        this.messageId = "trade" + System.currentTimeMillis();

        //reset l'état de transaction
        if(runningState == 4){
            runningState = 0;
        }

        myAgentCommercial.addBehaviour(new DealBehaviours());

        //Collect de propositions apres 150ms (rassuer la bonne réception de toutes les offres)
        myAgentCommercial.addBehaviour(new CollectProposalBehaviours(myAgentCommercial, 150));

        //Collect la confirmation d'achat apres 200ms
        myAgentCommercial.addBehaviour(new BuyMerchBehaviours(myAgentCommercial, 200));
    }

    /**
     * Choisir la meilleur quantité de produit à acheter
     * en fonction de l'argent disponible et quantité disponible de vendeur
     * @param price: prix unité
     * @param quantity: quantité proposée par le vendeur
     */
    private int moreSuitableQuantity(double price, int quantity) {
        return (int) Math.min(myAgentCommercial.getMoney() / price, quantity);
    }

    /**
     * Le comportement qui gère l'appel de proposition aupres de vendeur (CFP)
     */
    private class DealBehaviours extends OneShotBehaviour {

        @Override
        public void action() {
            lookingForDeal();
        }
    }

    /**
     * Le comportement collecte les propositions de vendeur et
     * choisit la meilleur proposition
     * Il envoie l'acceptation ACCEPT_PROPOSAL au gagnant
     * Il envoie le refus d'achat REFUSAL_PROPOSAL à les autres
     */
    private class CollectProposalBehaviours extends WakerBehaviour {

        public CollectProposalBehaviours(Agent a, long timeout) {
            super(a, timeout);
        }

        @Override
        protected void onWake() {
            super.onWake();
            handleProposal();
        }
    }

    /**
     * Ce comportement attends la confirmation d'achat de vendeur
     *
     */
    private class BuyMerchBehaviours extends WakerBehaviour {

        public BuyMerchBehaviours(Agent a, long timeout) {
            super(a, timeout);
        }

        @Override
        protected void onWake() {
            super.onWake();
            buyMerch(quanityIdeal, priceIdeal);
        }
    }

    /**
     * Cette fonction permet de traiter les confirmations d'achat de vendeur
     * Si la réponse de vendeur correspond à CONFIRM alors faire la mis à jours sur..
     * ..la quantité de marchandise à consommer et l'argent
     *
     * Sinon met l'état à 4 (fin)
     */
    public void buyMerch(int quantity, double price) {
        if (runningState == 3) {
            final MessageTemplate mtConfirm = MessageTemplate.MatchPerformative(ACLMessage.CONFIRM);
            final MessageTemplate mtCancel = MessageTemplate.MatchPerformative(ACLMessage.CANCEL);
            final MessageTemplate mtMatchingMsgId = MessageTemplate.MatchInReplyTo(messageId);
            final MessageTemplate matchAllPerformative = MessageTemplate.or(mtConfirm, mtCancel);
            final MessageTemplate mt = MessageTemplate.and(matchAllPerformative, mtMatchingMsgId);

            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                switch(msg.getPerformative()) {
                    case ACLMessage.CONFIRM:
                        logger.log(Logger.INFO, "[" + myAgentCommercial.getLocalName() + "]" + "Reçu --> ("+msg.getContent()+"): d'agent: "+msg.getSender().getLocalName(), this);
                        logger.log(Level.INFO, "[" + myAgentCommercial.getLocalName() + "]" + "Acheter "+quantity+ " " + myAgentCommercial.getConsumption() + " pour "+price+"/unité" + " de " + msg.getSender().getLocalName(), this);
                        int quantityUpdated = myAgentCommercial.getProductConsumed() + quantity;
                        double walletUpdated = myAgentCommercial.getMoney() - (price * quantity);
                        myAgentCommercial.setProductConsumed(quantityUpdated);
                        myAgentCommercial.setMoney(walletUpdated);
                        runningState = 4;
                        break;
                    case ACLMessage.CANCEL:
                        logger.log(Logger.INFO, "[" + myAgentCommercial.getLocalName() + "]" + "Reçu --> ("+msg.getContent()+"): d'agent: "+msg.getSender().getLocalName(), this);
                        runningState = 4;
                        break;
                    default:
                        break;
                }
            }
        }
    }


    /**
     * Cette méthode permet de choisir la meilleur offres
     * Cela collecte les réponses d'agents de type PROPOSE
     * Stocke tous les réponses et choisir le meilleur prix
     * Il prend la quantité disponible chez le vendeur et renvoyer la quantité convenable...
     * ..en fonction de l'argent restant
     *
     */
    private void handleProposal() {

        if (runningState == 1) {
            final MessageTemplate mtMatchingMsgId = MessageTemplate.MatchInReplyTo(messageId);
            final MessageTemplate mtMatchingPerformative = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
            final MessageTemplate mt = MessageTemplate.and(mtMatchingMsgId, mtMatchingPerformative);

            final List<ACLMessage> proposals = new ArrayList<>();
            ACLMessage proposal;

            while ((proposal = myAgent.receive(mt)) != null) {
                proposals.add(proposal);
                logger.log(Logger.INFO, "[" + myAgentCommercial.getLocalName() + "]" + "Reçu --> ("+proposal.getContent()+"): d'agent: "+proposal.getSender().getLocalName(), this);
            }

            ACLMessage bestProposal = null;
            for (ACLMessage p : proposals) {
                String demande = p.getContent();
                String[] demandeElements = demande.split(" ");
                int quantity = Integer.parseInt(demandeElements[1]);
                double price = Double.parseDouble(demandeElements[2]);

                if ((price < priceIdeal && quanityIdeal > 0) || bestProposal == null) {
                    priceIdeal = price;
                    bestProposal = p;
                    quanityIdeal = moreSuitableQuantity(price, quantity); //Choisir la quantité plus idéal
                }
            }

            if (bestProposal != null) {
                final List<ACLMessage> rejectList = new ArrayList<>();
                for (ACLMessage p : proposals) {
                    if (p != bestProposal) {
                        rejectList.add(p);
                    }
                }

                rejectProposals(rejectList);
                acceptProposal(bestProposal, quanityIdeal);
            }
        }
    }

    /**
     * Envoyer la demande d'achat au vendeur suite à la meilleur proposition
     * @param bestProposal: La meilleur proposition choisit dans la méthode handleProposal
     */
    private void acceptProposal(ACLMessage bestProposal, int quantity) {
        final ACLMessage response = bestProposal.createReply();
        response.setContent("ACCEPT_PROPOSAL " + quantity);
        response.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
        response.setReplyWith(messageId);
        myAgent.send(response);
        logger.log(Logger.INFO, "[" + myAgent.getLocalName() + "]" + "Envoyer ACCEPT_PROPOSAL à l'agent: " + bestProposal.getSender().getLocalName(), this);
        runningState = 3;
    }

    /**
     * Envoyer la refus d'achat au vendeur
     * @param proposals: La liste de propositions ne sont pas selectées comme la meilleur offre
     */
    private void rejectProposals(List<ACLMessage> proposals) {
        for (final ACLMessage proposal : proposals) {
            final ACLMessage response = proposal.createReply();
            logger.log(Logger.INFO, "[" + myAgent.getLocalName() + "]" + "Envoyer REJECT_PROPOSAL à l'agent: " + proposal.getSender().getLocalName(), this);
            //System.out.println("Sending NO to agent: " + proposal.getSender().getLocalName());
            response.setPerformative(ACLMessage.REJECT_PROPOSAL);
            response.setReplyWith(messageId);
            myAgent.send(response);
        }
    }

    /**
     * Demande de proposition de l'offre aupres des vendeurs
     */
    private void lookingForDeal() {
        searchingAgent();

        ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
        cfp.setReplyWith(messageId);
        for (int i = 0; i < productorAgents.length; ++i) {
            cfp.addReceiver(productorAgents[i]);
        }
        myAgent.send(cfp);
        runningState = 1;
    }

    /**
     * Stocke les vendeurs disponibles dans la liste productorAgents
     */
    private void searchingAgent() {
        //Chercher la liste de tous les agents
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("CFP " + myAgentCommercial.getConsumption());
        template.addServices(sd);

        try {
            DFAgentDescription[] result = DFService.search(myAgent, template);
            productorAgents = new AID[result.length];
            if (productorAgents.length == 0) {
                //notifyConsole("Aucun agent trouvé");
            } else {
                //notifyConsole("Trouvé les agents suivants: ");
            }

            for (int i = 0; i < result.length; i++) {
                productorAgents[i] = result[i].getName();
                //System.out.println(productorAgents[i].getName());
            }
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    public void notifyConsole(String toNotify) {
        System.out.println("[" + myAgent.getLocalName() + "] " + toNotify);
    }

}
