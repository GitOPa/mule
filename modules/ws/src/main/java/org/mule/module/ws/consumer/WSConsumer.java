/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.ws.consumer;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChainBuilder;
import org.mule.api.transformer.DataType;
import org.mule.api.transport.Connector;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.module.cxf.builder.ProxyClientMessageProcessorBuilder;
import org.mule.module.ws.security.SecurityStrategy;
import org.mule.module.ws.security.WSSecurity;
import org.mule.processor.ResponseMessageProcessorAdapter;
import org.mule.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.transformer.simple.AutoTransformer;

import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.wsdl.xml.WSDLReader;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.cxf.binding.soap.interceptor.CheckFaultInterceptor;

public class WSConsumer implements MessageProcessor
{

    private final MessageProcessor mp;
    private final String wsdlOperation;
    private final String targetNamespace;

    public WSConsumer(String wsdlLocation, String wsdlOperation, String serviceAddress, Connector connector,
                      WSSecurity security, MuleContext muleContext)
            throws MuleException, WSDLException, TransformerFactoryConfigurationError, TransformerException
    {

        WSDLReader wsdlReader11 = javax.wsdl.factory.WSDLFactory.newInstance().newWSDLReader();
        Definition wsdlDefinition = wsdlReader11.readWSDL(wsdlLocation);

        this.targetNamespace = wsdlDefinition.getTargetNamespace();
        this.wsdlOperation = wsdlOperation;

        ProxyClientMessageProcessorBuilder cxfBuilder = new ProxyClientMessageProcessorBuilder();
        cxfBuilder.setMuleContext(muleContext);

        if (security != null)
        {
            for (SecurityStrategy strategy : security.getStrategies())
            {
                strategy.apply(cxfBuilder);
            }
        }

        MessageProcessorChainBuilder chainBuilder = new DefaultMessageProcessorChainBuilder();
        AutoTransformer auto = new AutoTransformer();
        auto.setReturnDataType(DataType.STRING_DATA_TYPE);
        chainBuilder.chain(new ResponseMessageProcessorAdapter(auto));
        chainBuilder.chain(cxfBuilder.build(), createEndpoint(serviceAddress, connector, muleContext));
        mp = chainBuilder.build();

        // TODO: MULE-7222 The ProxyClientMessageProcessorBuilder removes this interceptor, we need it to throw an exception when a SOAPFault is returned.
        cxfBuilder.getClient().getEndpoint().getBinding().getInInterceptors().add(new CheckFaultInterceptor());
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        // TODO: MULE-7218 Read SOAPAction from wsdl
        event.getMessage().setOutboundProperty("SOAPAction", wsdlOperation);
        return mp.process(event);
    }

    protected OutboundEndpoint createEndpoint(String address, Connector connector, MuleContext muleContext)
            throws MuleException
    {
        if (connector != null)
        {
            String protocol = new MuleEndpointURI(address, muleContext).getScheme();
            if (!connector.supportsProtocol(protocol))
            {
                throw new RuntimeException("Connector " + connector + " does not support protocol: " + protocol);
            }
        }
        EndpointBuilder builder = muleContext.getEndpointFactory().getEndpointBuilder(address);
        builder.setConnector(connector);
        return muleContext.getEndpointFactory().getOutboundEndpoint(builder);
    }
}
