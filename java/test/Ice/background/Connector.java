// **********************************************************************
//
// Copyright (c) 2003-2009 ZeroC, Inc. All rights reserved.
//
// This copy of Ice is licensed to you under the terms described in the
// ICE_LICENSE file included in this distribution.
//
// **********************************************************************

final class Connector implements IceInternal.Connector
{
    public IceInternal.Transceiver
    connect()
    {
        _configuration.checkConnectException();
        return new Transceiver(_connector.connect());
    }

    public short
    type()
    {
        return (short)(EndpointI.TYPE_BASE + _connector.type());
    }

    public String
    toString()
    {
        return _connector.toString();
    }

    public int
    hashCode()
    {
        return _connector.hashCode();
    }

    //
    // Only for use by Endpoint
    //
    Connector(IceInternal.Connector connector)
    {
        _configuration = Configuration.getInstance();
        _connector = connector;
    }

    public boolean
    equals(java.lang.Object obj)
    {
        Connector p = null;

        try
        {
            p = (Connector)obj;
        }
        catch(ClassCastException ex)
        {
            return false;
        }

        if(this == p)
        {
            return false;
        }

        return _connector.equals(p._connector);
    } 

    final private IceInternal.Connector _connector;
    final private Configuration _configuration;
}
