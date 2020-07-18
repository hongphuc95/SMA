package behaviours;

import agents.AgentCommercial;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;

import java.util.logging.Level;
/**
 * @author Hong-Phuc VU
 * @author Yasmine GUEDJOU
 */


/**
 * Ce comportement gere la vente de produits d'agent
 * Il écoute toute le temps la demande d'achat par la boucle CyclicBehaviour
 * Dedans on trouve 2 autres comportement à type Oneshot ...
 * ...OfferRequestsServer et SellBehaviours
 */
public class SellerBehaviours extends CyclicBehaviour {
    int quantityOffer;
    double priceOffer;
    private java.util.logging.Logger logger;

    private AgentCommercial myAgentCommercial;

    @Override
    public void onStart() {
        super.onStart();
        this.myAgentCommercial = (AgentCommercial) myAgent;
        this.logger = Logger.getMyLogger(this.getClass().getName());
    }

    @Override
    public void action() {
        myAgentCommercial.addBehaviour(new OfferRequestsServer());
        myAgentCommercial.addBehaviour(new SellBehaviours());
    }

    /**
     * Le comportement envoie l'offre au acheteur
     * Le message à envoyer contient le prix proposé et le stockage produit à vendre disponible
     */
    private class OfferRequestsServer extends OneShotBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                try {
                    ACLMessage reply = msg.createReply();
                    //notifyConsole(msg.getSender().getLocalName() + " required product");
                    logger.log(Logger.INFO, "[" + myAgentCommercial.getLocalName() + "]" + "Reçu --> (CFP) d'agent: "+msg.getSender().getLocalName(), this);


                    int productAvailable = myAgentCommercial.getProductMade();
                    //System.out.println("productAvailable: " + productAvailable);
                    if (productAvailable > 0) {
                        reply.setPerformative(ACLMessage.PROPOSE);
                        //notifyConsole("Envoie l'offre à l'agent...");

                        //Propose le maximum de quantité possible
                        quantityOffer = (int)Math.max(myAgentCommercial.getProductMade(), 1);
                        priceOffer = myAgentCommercial.getPrice();
                        reply.setPerformative(ACLMessage.PROPOSE);
                        reply.setContent("PROPOSE "+quantityOffer+" "+priceOffer);
                        myAgent.send(reply);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                block();
            }
        }
    }

    /**
     * Le comportement SellBehaviours prends les acceptations de propositions par les acheteurs
     * Il envoie la confirmation de vente si il reste de stockage de produit par la méthode sendConfirm
     */
    private class SellBehaviours extends OneShotBehaviour {

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                logger.log(Logger.INFO, "[" + myAgentCommercial.getLocalName() + "]" + "Reçu --> ("+msg.getContent()+"): d'agent: "+msg.getSender().getLocalName(), this);
                sendConfirm(msg);
            }

        }
    }

    /**
     * Cette méthode envoie la confirmation d'achat au acheteur si il y a assez de stock
     * Mis a jour de l'argent et de stockage produit
     *
     * Si il n'y a pas assez de stock pour la vente alors envoie l'annulation d'achat
     * @param msg: Message pour l'acheteur
     */
    public void sendConfirm(ACLMessage msg) {
        int quantityNeeded;
        try {
            String[] msgContent = msg.getContent().split(" ");
            quantityNeeded = Integer.parseInt(msgContent[1]);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return;
        }

        //Check Quantity
        if (quantityNeeded <= myAgentCommercial.getProductMade()){
            //Send Confirm
            logger.log(Logger.INFO, "[" + myAgent.getLocalName() + "]" + "Reçu --> (Transaction valide! Send Confirm!): de :"+msg.getSender().getLocalName(), this);


            logger.log(Level.INFO,  "[" + myAgentCommercial.getLocalName() + "]" + "Vendre " + quantityNeeded+ " " + myAgentCommercial.getProduction() + " à "+priceOffer+"/unité" + " à " + msg.getSender().getLocalName(), this);
            int productMadeUpdated = myAgentCommercial.getProductMade() - quantityNeeded;
            double walletUpdated = myAgentCommercial.getMoney() + (priceOffer * quantityNeeded);
            myAgentCommercial.setMoney(walletUpdated);
            myAgentCommercial.setProductMade(productMadeUpdated);

            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.CONFIRM);
            reply.setContent("CONFIRM "+quantityNeeded+" "+priceOffer);
            myAgent.send(reply);

        } else {
            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.CANCEL);
            myAgent.send(reply);
            logger.log(Logger.INFO, myAgent.getLocalName() +"Reçu --> (Transaction invalide! Pas assez de produits): de :"+msg.getSender().getLocalName(), this);
        }
    }

    public void notifyConsole(String toNotify) {
        System.out.println("[" + myAgent.getLocalName() + "] " + toNotify);
    }
}
