package com.rackspace.contenttypefilter;

import java.util.Map;

import com.actional.soapstation.plugin.inproc.IInit;
import com.actional.soapstation.plugin.inproc.IInterceptor;
import com.actional.soapstation.plugin.inproc.IPluginInfo;

public class ActionalFilter implements IInterceptor, IPluginInfo, IInit {

    static final String uuidAccept = "1ef71842-bab9-8e05-4ea9-882aec746851-Accept";

	@Override
	public void destroy(IDestroyContext destroyContext) throws Exception {
	}

	@Override
	public void init(IInitContext initContext) throws Exception {
	}

	@Override
	public void getPluginInfo(IPluginInfoContext pluginInfoContext) throws Exception {
	}

    public String getHeader(Map<String, Object> headers, String header) {
        for(String key : headers.keySet()) {
            if(header.equalsIgnoreCase(key)) {
                return key;
            }
        }
        return null;
    }

	@Override
	public void invoke(IInvokeContext invokeContext) throws Exception {
		Map<String, Object> headers = invokeContext.getCallInfo().getTransportProperties();

		String uuidAcceptHeader = getHeader(headers, uuidAccept);

        if(uuidAcceptHeader != null) {
            String uuidAcceptHeaderValue = (String)headers.get(uuidAcceptHeader);

            headers.remove(uuidAcceptHeader);
            String acceptHeader = getHeader(headers, "accept");
            if(acceptHeader != null) {
                headers.remove(acceptHeader);
                headers.put(acceptHeader, uuidAcceptHeaderValue);
            }
            invokeContext.getCallInfo().setTransportProperties(headers, false);
        }
	}
}
