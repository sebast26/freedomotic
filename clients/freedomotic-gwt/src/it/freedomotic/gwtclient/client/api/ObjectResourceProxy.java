package it.freedomotic.gwtclient.client.api;

import it.freedomotic.model.object.EnvObject;

import org.restlet.client.resource.ClientProxy;
import org.restlet.client.resource.Get;
import org.restlet.client.resource.Result;

public interface ObjectResourceProxy extends ClientProxy{

	 @Get
	 public void retrieveObject(Result<EnvObject> callback);
	
}
