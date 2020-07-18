package agents;

import behaviours.SellerBehaviours;
import behaviours.TransactionBehaviours;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.util.Logger;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.ControllerException;
import tools.Config;

import java.util.ArrayList;
import java.util.Random;

/**
 * @author Hong-Phuc VU
 * @author Yasmine GUEDJOU
 */

public class AgentCommercial extends Agent {

    private static final long serialVersionUID = 1L;
    private Logger logger;

    private String production;
    private int productMade;
    private int maxStockProductMade;
    private double price;

    private String consumption;
    private int productConsumed;

    private double money;
    private double satisfaction;

    private int iteration = 0;
    private double avgPrice;
    private double avgSatisfaction;
    private double avgMoney;


    /**
     * Temps passé sans pouvoir consommé de produit
     */
    private double starvation;

    //Etat de l'agent
    private boolean isWorking = true;
    private int lifeState = 1; // 0 : survie, 1 : normal, 2 : reproduction

    long timePoint;

    @Override
    protected void setup() {
        super.setup();
        logger = Logger.getMyLogger(this.getClass().getName());

        //Initialise les variables
        init();
        registerDF();

        //Message du creation de l'agent
        logger.log(Logger.INFO, "Creer l'agent : " + this.getName(), this);

        //Ajout des classe Behviours

        //Mis a jour la satisfaction et le prix vendu de produit
        addBehaviour(new UpdateStat(this, 1000));

        //Consommation de marchandise
        addBehaviour(new ProductConsumedBehaviours(this, 1000));

        //Production de marchandise à vendre
        addBehaviour(new ProductMadeBehaviours(this, 1000));

        //Vente de marchandise
        addBehaviour(new SellerBehaviours());

        //Achat de marchandise
        addBehaviour(new TransactionBehaviours(this, 1000));

        //Affichage de l'argent disponible, la satisfaction actuelle,
        addBehaviour(new ReportBehaviours(this, 1000));

    }

    /**
     * Initialise les valeurs essentielles de l'agent
     */
    //OK
    public void init(){
        timePoint = System.currentTimeMillis();
        Object[] args = getArguments();

        if(args != null && args.length >= 2){
            production = (String)args[0];
            consumption = (String)args[1];
            notifyConsole("Production: " + production);
            notifyConsole("Consumption: " + consumption);
        } else {
            production = randomProduct();
            do {
                consumption = randomProduct();
            } while(consumption.equals(production));

            notifyConsole("Production: " + production);
            notifyConsole("Consumption: " + consumption);
        }

        productMade = 0;
        maxStockProductMade = Config.STOCK_MAX_PRODUCTION;

        productConsumed =  Config.INIT_CONSUMPTION;

        satisfaction = 100;
        money = Config.INIT_MONEY;
        price = Config.INIT_PRICE;

        avgMoney = money;
        avgPrice = price;
        avgSatisfaction = satisfaction;
    }

    /**
     * Enregistre l'agent dans le DF
     */
    //OK
    public void registerDF(){
        // Register the ComputeAgent service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("CFP "+ production);
        sd.setName(getName());
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
            logger.log(Logger.INFO, "Enregistre l'agent " + this.getLocalName() + " dans DF avec succès", this);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    /**
     * Supprime l'agent du DF
     */
    //OK
    public void deregister(){
        // Deregister from the yellow pages
        try {
            DFService.deregister(this);
            logger.log(Logger.INFO, "Agent" + this.getLocalName() + "is remove from DF!", this);
        }catch(FIPAException fe) {
            fe.printStackTrace();
        }
    }

    //NOT OK
    //TODO
    @Override
    protected void takeDown() {
        super.takeDown();
        deregister();
        //logger.log(Logger.INFO, "Destroy agent :"+this, this);
    }

    /**
     * Comportement de la production de la marchandise
     * L'agent produit une marchandise périodiquement
     */
    //OK
    public class ProductMadeBehaviours extends TickerBehaviour {

        public ProductMadeBehaviours(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            int total = 1;

            if (productMade < maxStockProductMade) {
                productMade = productMade + total;
                isWorking = true;
                //notifyConsole("un/une " + " est produit.");
            } else if (productMade == maxStockProductMade) {
                if (isWorking == true) {
                    satisfaction = 100;
                    logger.log(Logger.INFO, "[" + myAgent.getLocalName() + "]" + "Stockage Production MAX, STOP", this);
                    isWorking = false;
                }
            } else {
                //notifyConsole("Limit production MAX. Stop until all gone");
            }
        }
    }

    /**
     * Comportement de la consommation de la marchandise
     * L'agent consomme une marchandise périodiquement
     */
    //OK
    public class ProductConsumedBehaviours extends TickerBehaviour {

        public ProductConsumedBehaviours(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            int total = 1;

            if (productConsumed > 0) {
                productConsumed = productConsumed - total;
                //notifyConsole("On consomme un/une" +  " -- Stock restant: " + productConsumed);
            }
        }
    }

    public class ReportBehaviours extends TickerBehaviour {

        public ReportBehaviours(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            showInfo();
        }

        public void showInfo() {
            notifyConsole("Money: " + money + ", satisfaction: " + satisfaction + ", Price: " + price);
            notifyConsole("stockAvailableProduct: " + productMade + ", stockConsumed: " + productConsumed);
        }
    }

    /**
     * L'agent fait un rapport sur l'état d'agent
     */
    //OK
    public class UpdateStat extends TickerBehaviour {

        public UpdateStat(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            updatePrice();
            checkLifeState();
            checkSatisfaction();
            avgStat();
        }
    }

    /**
     * Mis à jour du prix de marchandise vendu
     */
    //OK
    public void updatePrice() {

        if(satisfaction == 100 && money >= Config.INIT_MONEY*2){
            price = price + (0.05 * Config.INIT_PRICE);
        } else if(satisfaction == 100 && money > Config.INIT_MONEY*1.5) {
            price = price + (0.15 * Config.INIT_PRICE);
        } else if(satisfaction < 90.0 && money < Config.INIT_MONEY*0.5){
            if (satisfaction < 30) {
                price = price * 0.5;
            } else if (satisfaction < 50) {
                price = price * 0.7;
            } else if (satisfaction < 80) {
                price = price * 0.8;
            } else if (satisfaction < 90) {
                price = price * 0.95;
            }
        }

        //TODO
    }

    /**
     * Mis à jour le niveau de vie de l'agent
     */
    //OK
    public void checkLifeState() {

        if(satisfaction < 90.0 && productConsumed < 3.0){
            lifeState = 3;
        }else if(satisfaction < 90.0 || productConsumed < 3.0){
            lifeState = 0;
        }else if(satisfaction == 100 && money >= Config.INIT_MONEY*1.5){
            lifeState = 2;
        }else{
            lifeState = 1;
        }
    }

    /**
     * Mis à jour du la satisfaction
     * Effectuer des modifications sur les autres éléments
     */
    //OK
    public void checkSatisfaction(){
        if(satisfaction <= 0.0){
            logger.log(Logger.INFO, "Agent : "+this.getName()+", est mourant! lifestate : "+this.getLifeState(), this);
            killAgent();
        }

        if(productConsumed <= 0){
            starvation = starvation + 1;
            satisfaction -= Math.exp( starvation /5.6 - 1.0);
            logger.log(Logger.FINE, "Agent : "+this.getName()+", Famine est augmenté à "+starvation+" !", this);
        } else {
            satisfaction = Math.min(satisfaction + 10, 100);
            starvation = 0;
        }
    }

    /**
     * Calculer la moyenne de satisfaction, de l'argent
     */
    public void avgStat() {
        iteration++;
        avgPrice = compute_average(avgPrice, price);
        avgSatisfaction = compute_average(avgSatisfaction, satisfaction);
        notifyConsole("Avg Satisfaction: " + avgSatisfaction);
        avgMoney = compute_average(avgMoney, money);
    }

    private double compute_average(double a, double b){
        return a + (b-a)/iteration;
    }

    /**
     * Temps d'execution
     * @return String(duration)
     */
    public String timeExec() {
        long timeNow = System.currentTimeMillis();
        long duration = (timeNow - timePoint)/1000;
        return String.valueOf(duration);
    }

    /**
     * Choisir la marchandise à vendre et à consommer par hasard ..
     * .. si l'utilisateur ne précise pas d'arguments
     * @return rand(book, paper, ink)
     */
    public String randomProduct() {
        ArrayList<String> products = new ArrayList();
        products.add("book");
        products.add("ink");
        products.add("paper");
        Random r = new Random();
        int index = r.nextInt(products.size());
        return products.get(index);

    }

    /**
     * Arret d'agent
     */
    //OK
    public void killAgent() {
        AgentContainer c = getContainerController();
        try {
            AgentController ac = c.getAgent(this.getAID().getLocalName());
            ac.kill();
        } catch (
                ControllerException e) {
            e.printStackTrace();
        }
    }

    //OK
    public void notifyConsole(String toNotify) {
        System.out.println("[" + this.getLocalName() + "] " + toNotify);
    }


     ////////////////////
    public String getConsumption() {
        return consumption.toString();
    }

    public int getMaxStockProductMade() {
        return maxStockProductMade;
    }

    public int getProductMade() {
        return productMade;
    }

    public int getProductConsumed() {
        return productConsumed;
    }

    public double getMoney() {
        return money;
    }

    public int getLifeState() {
        return lifeState;
    }

    public double getPrice() {
        return price;
    }

    public void setProductMade(int productMade) {
        this.productMade = productMade;
    }

    public void setProductConsumed(int productConsumed) {
        this.productConsumed = productConsumed;
    }

    public void setMoney(double money) {
        this.money = money;
    }

    public String getProduction() {
        return production;
    }

}
