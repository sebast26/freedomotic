package it.freedomotic.gwtclient.client.api;

import it.freedomotic.model.environment.Environment;
import it.freedomotic.model.environment.Zone;
import it.freedomotic.model.object.Behavior;
import it.freedomotic.model.object.BooleanBehavior;
import it.freedomotic.model.object.EnvObject;
import it.freedomotic.model.object.ListBehavior;
import it.freedomotic.model.object.RangedIntBehavior;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.restlet.client.resource.Result;

import com.furiousbob.jms.client.StompClient;
import com.furiousbob.jms.client.StompJS;
import com.google.gwt.core.client.GWT;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.Window;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.ListDataProvider;


public class EnvironmentController {

	public static final int STOMP_ERROR = 0;
	public static final int REST_ERROR = 1;
	public static final int CONNECTED= 2;
	public static final String ROOT_URL= "/gwt_client/v1"; 
	
	private static EnvironmentController INSTANCE=null;  //Singleton reference
	private static EnvironmentResourceProxy environmentResource;			
	private Environment environment;
	private ArrayList<Zone> rooms;
	private boolean hasData= false;
	
	private List<EnvObject> freedomObjects = new ArrayList<EnvObject>();	
	private HashMap<String, EnvObject> freedomObjectsDictionary;
	
    private static String broker_ip;
	
	private static StompClient sc;
	public static ACStompCallback cb;
	/**
	   * The provider that holds the list of objects.
	   */
	private ListDataProvider<EnvObject> envObjectDataProvider = new ListDataProvider<EnvObject>();
	
	private ListDataProvider<Zone> zoneDataProvider = new ListDataProvider<Zone>();
	  
	 // Private constructor suppresses 
    private EnvironmentController() {    		
    }
 
    // Sync creator to avoid multi-thread problems
    private static void createInstance() {
        if (INSTANCE == null) { 
            INSTANCE = new EnvironmentController();
        }
    }
 
    public static EnvironmentController getInstance() {
        if (INSTANCE == null) createInstance();
        return INSTANCE;
    }
    
    public int init()
    {
    	if(!prepareRestResource())
    		return REST_ERROR;
    	return CONNECTED;
    }
    
    //TODO: create a generic client to retrieve all resources. See restlet project.
    public static boolean prepareRestResource()
    {
    	environmentResource = GWT.create(EnvironmentResourceProxy.class);
		// Set up the contact resource
    	environmentResource.getClientResource().setReference("v1/environment/");    	   
        //TODO: Find how to check the configuration
        return true; 
    }
    
    public static boolean initStomp()
    {
    	//First we init the stomp
		try {
			StompJS.install();
			GWT.log("stomp.js installed.");
			//Window.alert("stomp.js installed.");
			
			
			cb = new ACStompCallback();			
			//Window.alert("hostnamevalue "+value);
			//sc = new StompClient("ws://"+value+":61614/stomp", cb);
			sc = new StompClient("ws://"+broker_ip+":61614/stomp", cb);
			cb.setClient(sc);
			sc.connect();
			return true;
														
		} catch (Throwable t) {
			GWT.log(t.getMessage(), t);
			return false;
		}	
    	
    }
    public static void changeBehavior(String object, String behavior, String value){
		String text = createXMLCommand(object, behavior, value);				
		if (cb.isConnected())
		{	
			String queue = "/queue/app.events.sensors.behavior.request.objects";
			JSONObject header = new JSONObject();
			header.put("transformation", new JSONString("jms-object-xml"));						
			//Message msg = Message.create(text);					
			sc.send(queue, text,header.getJavaScriptObject());				
		}
		else
		{
			GWT.log("no conected");
			
		}
    	
    	
    	
    }
	public static String createXMLCommand(String object, String behavior, String value)
	{
     // String queue = "/queue/app.events.sensors.behavior.request.objects";
      String command ="<it.freedomotic.reactions.Command>"+
              "   <name>StompClientCommand</name>"+
              "   <delay>0</delay>"+
              "   <timeout>2000</timeout>"+
              "   <hardwareLevel>false</hardwareLevel>"+
              "   <description>test</description>"+
              "   <receiver>app.events.sensors.behavior.request.objects</receiver>"+
              "	<properties>" +
              "	    <properties>" +
              "      		<property name=\"object\" value=\""+object+"\"/>" +
              "	        <property name=\"behavior\" value=\""+behavior+"\"/>" +
              "      		<property name=\"value\" value=\""+value+"\"/>" +
              "	    </properties>" +
              "	</properties>" +                  
              "</it.freedomotic.reactions.Command>"; 
      return command;
		
	}
    
    public void retrieve() 
	{		
    	// Retrieve the contact
		GWT.log("retrieve data");
    	environmentResource.retrieve(new Result<Environment>() {
		    public void onFailure(Throwable caught) {
		        //TODO: Handle the error
		    }		   
			@Override
			public void onSuccess(Environment result) {
				GWT.log("retrieve data: success");
				environment = result;
				hasData =true;
				rooms= new ArrayList<Zone>();
				freedomObjectsDictionary = new HashMap<String, EnvObject>();
				for(Zone z: getEnvironment().getZones())
				{
					if (z.isRoom())
					{
						rooms.add(z);
					}
					for (EnvObject obj: z.getObjects())
						freedomObjectsDictionary.put(obj.getName(), obj);
				}
				//roomsSize=rooms.size();
																			
				List<EnvObject> list = envObjectDataProvider.getList();
				list.clear();
				list.addAll(freedomObjectsDictionary.values());
				
				List<Zone> list2 = zoneDataProvider.getList();
				list2.clear();
				list2.addAll(rooms);
			}
		});						
	}
     
    public ListDataProvider<EnvObject> getEnvObjectDataProvider()
    {
    	return envObjectDataProvider;
    	
    }
	public void addEnvObjectDataDisplay(HasData<EnvObject> display) {
		envObjectDataProvider.addDataDisplay(display);		
	}	
	 /**
	   * Refresh all displays.
	   */
	public void refreshEnvObjectDisplays() {
		envObjectDataProvider.refresh();
	}
	
	public ListDataProvider<Zone> getZoneDataProvider()
    {
    	return zoneDataProvider;
    	
    }
	public void addZoneDataDisplay(HasData<Zone> display) {
		zoneDataProvider.addDataDisplay(display);		
	}	
	 /**
	   * Refresh all displays.
	   */
	public void refreshZoneDisplays() {
		zoneDataProvider.refresh();
	}
			
   	public static void message(String message) {   		
		Payload payload = FreedomoticStompHelper.parseMessage(message);
		final EnvObject obj = EnvironmentController.getInstance().getObject(payload.getStatements("object.name").get(0).getValue());
//	//TODO: This way is not very efficient, as we are ignoring the message and retrieving the data again.
//		ObjectResourceProxy objectResource;		
//		final EnvObject new_obj; 	
//		
//		objectResource = GWT.create(ObjectResourceProxy.class);
//		// Set up the contact resource
//		objectResource.getClientResource().setReference("v1/environment/objects/"+payload.getStatements("object.name").get(0).getValue());
//		
//		objectResource.retrieveObject(new Result<EnvObject>() { 
//			public void onFailure(Throwable caught) {
//	        //TODO: Handle the error
//			}		   
//		@Override
//		public void onSuccess(EnvObject result) {
//			for (Behavior bh: obj.getBehaviors())
//			{								 
//				Behavior resultBehavior = result.getBehavior(bh.getName());				
//				if (bh instanceof BooleanBehavior)
//				{					
//					((BooleanBehavior) bh).setValue(((BooleanBehavior)resultBehavior).getValue());
//				}
//				else if (bh instanceof RangedIntBehavior)
//				{					
//					((RangedIntBehavior) bh).setValue(((RangedIntBehavior)resultBehavior).getValue());
//				}
//				else if (bh instanceof ListBehavior)
//				{
//					((ListBehavior) bh).setSelected(((ListBehavior)resultBehavior).getSelected());	
//				}							
//			}
//			obj.setCurrentRepresentation(result.getCurrentRepresentationIndex());		
//		}
//		});
    	
//	This should be the correct way, but the problem is with behavior linked changes. Review this way if the messages change 
		
		Iterator it = payload.iterator();
        while (it.hasNext()) {
            Statement st = (Statement) it.next();	            
			if (st.getAttribute().equalsIgnoreCase("object.currentRepresentation"))
			{    							
				if (obj.getCurrentRepresentationIndex() !=  Integer.parseInt(st.getValue()))
				{
					obj.setCurrentRepresentation(Integer.parseInt(st.getValue()));					
				}    				    				
			}    			    			    			
			else if (!st.getAttribute().equalsIgnoreCase("object.name"))
			{ 
				Behavior bh = obj.getBehavior(st.getAttribute());							 
				if (bh instanceof BooleanBehavior)
				{
					boolean bl = Boolean.parseBoolean(st.getValue()); 
					if (bl !=((BooleanBehavior)bh).getValue())
					{
						((BooleanBehavior) bh).setValue(bl);
						//Window.alert(obj.toString());
					}

				}
				else if (bh instanceof RangedIntBehavior)
				{					
					int val = Integer.parseInt(st.getValue());
					if (val !=((RangedIntBehavior)bh).getValue())
					{
						((RangedIntBehavior) bh).setValue(val);
						//Window.alert(obj.toString());
					}								 								 
				}
				else if (bh instanceof ListBehavior)
				{
					String val = st.getValue();
					if (!val.equals(((ListBehavior)bh).getSelected()))
						((ListBehavior)bh).setSelected(val);						
				}
			}    		
		}
   	}
	public boolean HasData() {
		return hasData;
	}

	public Environment getEnvironment() {
		return environment;
	}
	public EnvObject getObject(String objectName)
	{			
		return freedomObjectsDictionary.get(objectName);
		
	}
	public static String getBrokerIp()
	{
		return broker_ip;
		
	}
	public static void setBrokerIp(String brokerIp)
	{
		broker_ip=brokerIp;
		
	}
}
