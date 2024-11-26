package examples.yellowPages;
import jade.core.*;
import jade.core.behaviours.*;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.domain.DFService;
import jade.domain.FIPANames;


public class SubDF extends jade.domain.df {

  
  public void setup() {

   // Input df name
   int len = 0;
   byte[] buffer = new byte[1024];

   try {

     AID parentName = getDefaultDF(); 
     
     //Execute the setup of jade.domain.df which includes all the default behaviours of a df 
     //(i.e. register, unregister,modify, and search).
     super.setup();
    
     //Use this method to modify the current description of this df. 
     setDescriptionOfThisDF(getDescription());
     
     //Show the default Gui of a df.
     super.showGui();

     DFService.register(this,parentName,getDescription());
     addParent(parentName,getDescription());
		 System.out.println("Agent: " + getName() + " federated with default df.");
     
    }catch(FIPAException fe){fe.printStackTrace();}
  }
  
  private DFAgentDescription getDescription()
  {
     DFAgentDescription dfd = new DFAgentDescription();
     dfd.setName(getAID());
     ServiceDescription sd = new ServiceDescription();
     sd.setName(getLocalName() + "-sub-df");
     sd.setType("fipa-df");
     sd.addProtocols(FIPANames.InteractionProtocol.FIPA_REQUEST);
     sd.addOntologies("fipa-agent-management");
     sd.setOwnership("JADE");
     dfd.addServices(sd);
     return dfd;
  }

}
